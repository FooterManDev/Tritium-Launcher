package auth

import Constants.CLIENT_ID
import auth.MicrosoftAuth.getMCProfile
import auth.MicrosoftAuth.isSignedIn
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import toURI
import java.awt.Desktop
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.swing.JOptionPane

/**
 * Contains all logic for Minecraft authentication.
 *
 * @property isSignedIn Whether the user is signed in
 *
 * @property getMCProfile Get the user's [MCProfile]
 *
 * TODO: Clean up
 */
object MicrosoftAuth {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private const val TOKEN_FILE = "token.bin"
    private const val REFRESH_TOKEN_FILENAME = "refresh_token.bin"

    private const val REFRESH_BUFFER_MS = 60 * 1000L

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json)
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var tokenExpiryTime: Long = 0L
    @Volatile private var _isSignedIn = false
    val isSignedIn: Boolean get() = _isSignedIn

    private const val DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
    private const val TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
    private const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    private const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"

    private fun saveToken(token: String) {
        try {
            val encrypted = CryptoMngr.encrypt(token)
            Files.write(Paths.get(TOKEN_FILE), encrypted.toByteArray(StandardCharsets.UTF_8))
            logger.info("Access token saved.")
        } catch (e: Exception) {
            logger.error("Failed to save access token: ${e.message}", e)
            throw e
        }
    }

    private fun loadToken(): String? {
        return try {
            if (Files.exists(Paths.get(TOKEN_FILE))) {
                val encrypted = String(Files.readAllBytes(Paths.get(TOKEN_FILE)), StandardCharsets.UTF_8)
                val token = CryptoMngr.decrypt(encrypted)
                logger.info("Access token loaded.")
                token
            } else null
        } catch (e: Exception) {
            logger.error("Failed to load access token: ${e.message}", e)
            null
        }
    }

    private fun deleteToken() {
        try {
            Files.deleteIfExists(Paths.get(TOKEN_FILE))
            logger.info("Access token deleted.")
        } catch (e: Exception) {
            logger.error("Error deleting access token: ${e.message}", e)
        }
    }

    private fun saveRefreshToken(token: String) {
        try {
            val encrypted = CryptoMngr.encrypt(token)
            Files.write(Paths.get(REFRESH_TOKEN_FILENAME), encrypted.toByteArray(StandardCharsets.UTF_8))
            logger.info("Refresh token saved.")
        } catch (e: Exception) {
            logger.error("Failed to save refresh token: ${e.message}", e)
            throw e
        }
    }

    private fun loadRefreshToken(): String? {
        return try {
            if (Files.exists(Paths.get(REFRESH_TOKEN_FILENAME))) {
                val encrypted = String(Files.readAllBytes(Paths.get(REFRESH_TOKEN_FILENAME)), StandardCharsets.UTF_8)
                val token = CryptoMngr.decrypt(encrypted)
                logger.info("Refresh token loaded.")
                token
            } else null
        } catch (e: Exception) {
            logger.error("Failed to load refresh token: ${e.message}", e)
            null
        }
    }

    private fun deleteRefreshToken() {
        try {
            Files.deleteIfExists(Paths.get(REFRESH_TOKEN_FILENAME))
        } catch (e: Exception) {
            logger.error("Error deleting refresh token: ${e.message}", e)
        }
    }

    suspend fun getMCProfile(): MCProfile? {
        val token = ensureValidAccessToken()
        return fetchMCProfile(token)
    }

    suspend fun getMCProfileWithUUID(uuid: String): MCPlayerInfo? {
        val response = httpClient.get("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun signIn(onSignedIn: (MCProfile?) -> Unit) {
        try {
            logger.info("Starting sign-in flow...")
            val deviceCodeResp = getDeviceCode()
            showVerificationInstructions(deviceCodeResp.verificationUri, deviceCodeResp.userCode)

            val tokenResponse = pollForAccessToken(deviceCodeResp)

            val (xblToken, hash) = authWithLive(tokenResponse.accessToken)
            val xstsToken = authWithXSTS(xblToken)

            val mcToken = getMCToken(xstsToken, hash)

            saveToken(mcToken)
            tokenResponse.refreshToken?.let {
                saveRefreshToken(it)
            }

            tokenExpiryTime = System.currentTimeMillis() + (tokenResponse.expiresIn.takeIf { it > 0 } ?: 3600) * 1000L
            _isSignedIn = true

            logger.info("Sign-in successful. Token expires in ${tokenResponse.expiresIn} seconds.")
            val profile = fetchMCProfile(mcToken)
            onSignedIn(profile)
        } catch (e: Exception) {
            logger.error("Sign-in failed: ${e.message}", e)
            signOut()
            throw e
        }
    }

    suspend fun ensureValidAccessToken(): String {
        val currentToken = loadToken() ?: throw IllegalStateException("No access token; sign in required.")
        if(System.currentTimeMillis() >= tokenExpiryTime - REFRESH_BUFFER_MS) {
            logger.info("Access token near expiry; refreshing.")
            return refreshAccessTokenIfNeeded()
        }
        return currentToken
    }

    suspend fun refreshAccessTokenIfNeeded(): String {
        val storedRefresh = loadRefreshToken() ?: throw IllegalStateException("No refresh token; sign in required.")
        return try {
            refreshAccessToken(storedRefresh)
        } catch (e: Exception) {
            logger.error("Token refresh error: ${e.message}", e)
            deleteToken()
            deleteRefreshToken()
            _isSignedIn = false
            throw Exception("Token refresh failed; try sign in again.", e)
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): String {
        val params = listOf(
            "client_id" to CLIENT_ID,
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "scope" to "XboxLive.signin offline_access"
        ).formUrlEncode()

        logger.info("Requesting new access token via refresh token.")
        val response: HttpResponse = try {
            httpClient.post(TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(params)
            }
        } catch (e: Exception) {
            throw Exception("Network error during token refresh: ${e.message}", e)
        }
        val body = response.bodyAsText()
        if(response.status == HttpStatusCode.OK) {
            val tokenResponse: TokenResponse = try {
                json.decodeFromString(TokenResponse.serializer(), body)
            } catch (e: Exception) {
                throw Exception("Failed to parse token refresh response: ${e.message}", e)
            }
            tokenExpiryTime = System.currentTimeMillis() + tokenResponse.expiresIn * 1000L
            saveToken(tokenResponse.accessToken)
            tokenResponse.refreshToken?.let { saveRefreshToken(it) }
            logger.info("Token refresh successful; new token expires in ${tokenResponse.expiresIn} seconds.")
            return tokenResponse.accessToken
        } else throw Exception("Token refresh failed with status ${response.status.value}: $body")
    }

    private suspend fun getDeviceCode(): DeviceCodeResponse {
        return try {
            val response: HttpResponse = httpClient.post(DEVICE_CODE_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    listOf(
                        "client_id" to CLIENT_ID,
                        "scope" to "XboxLive.signin offline_access"
                    ).formUrlEncode()
                )
            }

            if (response.status.value != 200) {
                val errBody = response.bodyAsText()
                logger.error("Failed to get device code: $errBody")
                throw IllegalStateException("Failed to get device code: $errBody")
            }

            val responseBody = response.bodyAsText()
            logger.info("Device code response received: ${response.bodyAsText()}")
            val deviceCodeResponse: DeviceCodeResponse = try {
                json.decodeFromString(DeviceCodeResponse.serializer(), responseBody)
            } catch (e: Exception) {
                logger.error("Failed to parse device code response: ${e.message}", e)
                throw IllegalStateException("Failed to parse device code response", e)
            }

            deviceCodeResponse
        } catch (e: Exception) {
            logger.error("Error getting device code: ${e.message}", e)
            throw e
        }
    }

    private fun showVerificationInstructions(verificationUri: String, userCode: String) {
        if(Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(verificationUri.toURI())
        }
        JOptionPane.showMessageDialog(
            null,
            "1. Open the URL in your browser: $verificationUri\n" +
            "2. Enter the code: $userCode\n" +
            "3. Log into your MS account.", // Message
            "Device Authentication", // Title
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private suspend fun pollForAccessToken(code: DeviceCodeResponse): TokenResponse {
        val interval = code.interval * 1000L
        while(true) {
            delay(interval)

            val response: HttpResponse = try {
                httpClient.post(TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf(
                            "client_id" to CLIENT_ID,
                            "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                            "device_code" to code.deviceCode,
                            "scope" to "XboxLive.signin offline_access"
                        ).formUrlEncode()
                    )
                }
            } catch (e: Exception) {
                logger.error("Network error while polling for token: ${e.message}", e)
                continue
            }
            val body = response.bodyAsText()
            if(response.status == HttpStatusCode.OK) {
                val tokenResponse: TokenResponse = try {
                    json.decodeFromString(body)
                } catch (e: Exception) {
                    logger.error("Error parsing token response: ${e.message}", e)
                    throw Exception("Failed to parse token response", e)
                }
                logger.info("Token acquired successfully. Expires in ${tokenResponse.expiresIn} seconds.")
                return tokenResponse
            } else if(response.status == HttpStatusCode.BadRequest) {
                val errorResponse = try {
                    json.decodeFromString<DeviceCodeErrorResponse>(response.bodyAsText())
                } catch (e: Exception) {
                    logger.error("Error parsing error response: ${e.message}", e)
                    throw Exception("Failed to parse error response", e)
                }
                if(errorResponse.error == "authorization_pending") {
                    logger.info("Authorization pending, continuing to poll...")
                    continue
                } else {
                    logger.error("Error in token polling: ${errorResponse.error}")
                    throw IllegalStateException(errorResponse.error)
                }
            } else {
                logger.error("Unexpected HTTP status ${response.status.value}: $body")
                throw IllegalStateException("Unexpected response")
            }
        }
    }

    private suspend fun authWithLive(token: String): Pair<String, String> {
        val responseBody = XblAuthRequest(
            Properties = Properties(
                AuthMethod = "RPS",
                SiteName = "user.auth.xboxlive.com",
                RpsTicket = "d=$token"
            ),
            RelyingParty = "http://auth.xboxlive.com",
            TokenType = "JWT"
        )

        return try {
            val response: HttpResponse = httpClient.post(XBL_AUTH_URL) {
                contentType(ContentType.Application.Json)
                setBody(responseBody)
            }

            if(response.status != HttpStatusCode.OK) {
                val errBody = response.bodyAsText()
                logger.error("XBL authentication failed with HTTP status ${response.status.value}: $errBody")
                throw IllegalStateException("XBL authentication failed with HTTP status ${response.status.value}")
            }

            logger.info("XBL auth response received: ${response.bodyAsText()}")
            val xblResponse: XblTokenResponse = try {
                json.decodeFromString(XblTokenResponse.serializer(), response.bodyAsText())
            } catch (e: Exception) {
                logger.error("Failed to parse XBL auth response: ${e.message}", e)
                throw IllegalStateException("XBL auth response parsing failed", e)
            }

            val xblToken = xblResponse.token
            val hash = xblResponse.displayClaims.xui.first().userHash

            Pair(xblToken, hash)
        } catch (e: Exception) {
            logger.error("Error in authWithLive: ${e.message}", e)
            throw e
        }
    }

    private suspend fun authWithXSTS(token: String): String {
        val responseBody = XstsAuthRequest(
            Properties = XstsProperties(
                SandboxId = "RETAIL",
                UserTokens = listOf(token)
            ),
            RelyingParty = "rp://api.minecraftservices.com/",
            TokenType = "JWT"
        )

        return try {
            val response: HttpResponse = httpClient.post(XSTS_AUTH_URL) {
                contentType(ContentType.Application.Json)
                setBody(responseBody)
            }

            if(response.status != HttpStatusCode.OK) {
                val errBody = response.bodyAsText()
                logger.error("XSTS authentication failed with HTTP status ${response.status.value}: $errBody")
                throw IllegalStateException("XSTS authentication failed with HTTP status ${response.status.value}")
            }

            logger.info("XSTS auth response received: ${response.bodyAsText()}")
            val xstsResponse: XstsTokenResponse = try {
                json.decodeFromString(XstsTokenResponse.serializer(), response.bodyAsText())
            } catch (e: Exception) {
                logger.error("Failed to parse XSTS auth response: ${e.message}", e)
                throw IllegalStateException("XSTS auth response parsing failed", e)
            }

            xstsResponse.token
        } catch (e: Exception) {
            logger.error("Error in authWithXSTS: ${e.message}", e)
            throw e
        }
    }

    private suspend fun getMCToken(token: String, hash: String): String {
        val identity = "XBL3.0 x=$hash;$token"
        val body = mapOf("identityToken" to identity)

        return try {
            val response: HttpResponse =
                httpClient.post("https://api.minecraftservices.com/authentication/login_with_xbox") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

            if (response.status != HttpStatusCode.OK) {
                val errBody = response.bodyAsText()
                logger.error("MC authentication failed with HTTP status ${response.status.value}: $errBody")
                throw IllegalStateException("MC authentication failed with HTTP status ${response.status.value}")
            }

            val responseBody = response.bodyAsText()
            logger.info("MC auth response received: $responseBody")
            val authResponse: MCAuthResponse = try {
                json.decodeFromString(MCAuthResponse.serializer(), responseBody)
            } catch (e: Exception) {
                logger.error("Failed to parse MC auth response: ${e.message}", e)
                throw IllegalStateException("MC auth response parsing failed", e)
            }

            authResponse.accessToken
        } catch (e: Exception) {
            logger.error("Error in getMCToken: ${e.message}", e)
            throw e
        }
    }

    private suspend fun fetchMCProfile(token: String): MCProfile? {
        return try {
            val response: HttpResponse = httpClient.get(MC_PROFILE_URL) {
                header("Authorization", "Bearer $token")
            }

            if(response.status != HttpStatusCode.OK) {
                val errBody = response.bodyAsText()
                logger.error("MC profile fetch failed with HTTP status ${response.status.value}: $errBody")
                throw IllegalStateException("MC profile fetch failed with HTTP status ${response.status.value}")
            }

            val responseBody = response.bodyAsText()
            logger.info("MC profile response received: $responseBody")
            val profileResponse: MCProfile = try {
                json.decodeFromString(MCProfile.serializer(), responseBody)
            } catch (e: Exception) {
                logger.error("Failed to parse MC profile response: ${e.message}", e)
                throw IllegalStateException("MC profile response parsing failed", e)
            }

            profileResponse
        } catch (e: Exception) {
            logger.error("Error in fetchMCProfile: ${e.message}", e)
            throw e
        }
    }

    fun signOut() {
        deleteToken()
        deleteRefreshToken()
        _isSignedIn = false
        logger.info("User signed out.")
    }

    suspend fun getUUID(name: String): String? {
        try {
            val response: HttpResponse = httpClient.get("https://api.mojang.com/users/profiles/minecraft/$name")
            return Json.decodeFromString<UUID>(response.bodyAsText()).id
        } catch (e: Exception) {
            logger.error("Error getting UUID: ${e.message}", e)
            return null
        }
    }

    suspend fun getSkinAndCapeTextures(uuid: String): Texture? {
        val profile = getMCProfileWithUUID(uuid)
        if(profile != null) {
            val properties = profile.properties
            if(properties.name == "textures") {
                val textures = Base64.getDecoder().decode(properties.value).toString(Charsets.UTF_8)
                return json.decodeFromString<Texture>(textures)
            }
        }
        logger.error("Error getting skin and cape textures")
        return null
    }

    suspend fun getMCSkinUrl(uuid: String): String? {
        try {
            val textures = getSkinAndCapeTextures(uuid)
            return textures?.textures?.SKIN?.url
        } catch (e: Exception) {
            logger.error("Error getting skin URL: ${e.message}", e)
            return null
        }
    }

    suspend fun getMCCapeUrl(uuid: String): String? {
        try {
            val textures = getSkinAndCapeTextures(uuid)
            return textures?.textures?.CAPE?.url
        } catch (e: Exception) {
            logger.error("Error getting cape URL: ${e.message}", e)
            return null
        }
    }
}

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("interval") val interval: Int
)

@Serializable
data class DeviceCodeErrorResponse(val error: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("scope") val scope: String,
    @SerialName("refresh_token") val refreshToken: String? = null
)

@Serializable
data class XblTokenResponse(
    @SerialName("Token") val token: String,
    @SerialName("DisplayClaims") val displayClaims: DisplayClaims
)

@Serializable
data class XblAuthRequest(
    val Properties: Properties,
    val RelyingParty: String,
    val TokenType: String
)

@Serializable
data class XstsAuthRequest(
    val Properties: XstsProperties,
    val RelyingParty: String,
    val TokenType: String
)

@Serializable
data class Properties(
    val AuthMethod: String,
    val SiteName: String,
    val RpsTicket: String
)

@Serializable
data class XstsProperties(
    val SandboxId: String,
    val UserTokens: List<String>
)

@Serializable
data class XstsTokenResponse(
    @SerialName("Token") val token: String,
    @SerialName("DisplayClaims") val displayClaims: DisplayClaims
)

@Serializable
data class DisplayClaims(
    @SerialName("xui") val xui: List<Xui>
)

@Serializable
data class Xui(
    @SerialName("uhs") val userHash: String
)

@Serializable
data class MCAuthResponse(
    @SerialName("username") val uuid: String, // this is not the player UUID.
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class MCProfile(
    val id: String,
    val name: String,
    @SerialName("skins") val skins: List<Skin>,
    @SerialName("capes") val capes: List<Cape>
)

@Serializable
data class Cape(
    val id: String,
    val state: String,
    val url: String,
    @SerialName("alias") val alias: String? = null
)

@Serializable
data class Skin(
    val id: String,
    val url: String,
    val state: String,
    val variant: String,
    @SerialName("alias") val alias: String? = null
)

@Serializable
data class UUID(
    val id: String,
    val name: String,
    val legacy: Boolean,
    val demo: Boolean
)

@Serializable
data class MCPlayerInfo(
    val id: String,
    val name: String,
    val legacy: Boolean,
    val properties: MCPlayerProperties
)

@Serializable
data class MCPlayerProperties(
    val name: String, // The only property that exists is "textures".
    val signature: String,
    val value: String // Decoding this value results in the Texture object.
)

@Serializable
data class Texture(
    val timestamp: Int, // Unix time in milliseconds.
    val profileId: String,
    val profileName: String,
    val signatureRequired: Boolean,
    val textures: Textures
)

@Serializable
data class Textures(
    val SKIN: SkinTexture,
    val CAPE: CapeTexture
)

@Serializable
data class SkinTexture(
    val url: String,
    val metadata: SkinModel
)

@Serializable
data class CapeTexture(
    val url: String
)

@Serializable
data class SkinModel(
    val model: String
)