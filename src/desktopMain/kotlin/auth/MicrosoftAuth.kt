package auth

import Constants.CLIENT_ID
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
import kotlinx.serialization.json.JsonElement
import toURI
import java.awt.Desktop
import java.io.FileInputStream
import java.util.*
import javax.swing.JOptionPane

object MicrosoftAuth {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json)
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    private const val DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
    private const val TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
    private const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    private const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"

    suspend fun auth(): MCProfile? {
        val deviceCodeResp = getDeviceCode()
        showVerificationInstructions(deviceCodeResp.verificationUri, deviceCodeResp.userCode)

        val token = pollForAccessToken(deviceCodeResp)
        val (xblToken, hash) = authWithLive(token)
        val xstsToken = authWithXSTS(xblToken)
        val mcToken = getMCToken(xstsToken, hash)
        return fetchMCProfile(mcToken)
    }

    private suspend fun getDeviceCode(): DeviceCodeResponse {
        val response: HttpResponse = httpClient.post(DEVICE_CODE_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "client_id" to CLIENT_ID,
                    "scope" to "XboxLive.signin offline_access"
                ).formUrlEncode()
            )
        }

        val body = response.bodyAsText()
        println(body)

        if(response.status.value != 200) {
            throw IllegalStateException("Failed to get device code: $body")
        }

        return json.decodeFromString(body)
    }

    private fun showVerificationInstructions(verificationUri: String, userCode: String) {
        if(Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(verificationUri.toURI())
        }
        JOptionPane.showMessageDialog(
            null,
            "1. Open the URL in your browser: $verificationUri\n" +
            "2. Enter the code: $userCode\n" +
            "3. Log into your MS account.",
            "Device Authentication",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private suspend fun pollForAccessToken(code: DeviceCodeResponse): String {
        val interval = code.interval * 1000L
        while(true) {
            delay(interval)

            val response: HttpResponse = httpClient.post(TOKEN_URL) {
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

            println(response.bodyAsText())

            if(response.status == HttpStatusCode.OK) {
                val tokenResponse: TokenResponse = json.decodeFromString(response.bodyAsText())
                return tokenResponse.accessToken
            }

            if(response.status == HttpStatusCode.BadRequest) {
                val errorResponse = json.decodeFromString<DeviceCodeErrorResponse>(response.bodyAsText())
                if(errorResponse.error == "authorization_pending") {
                    continue
                } else throw IllegalStateException(errorResponse.error)
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

        val response: HttpResponse = httpClient.post(XBL_AUTH_URL) {
            contentType(ContentType.Application.Json)
            setBody(responseBody)
        }

        println("fn::authWithLive" + response.bodyAsText())

        val xblResponse: XblTokenResponse = json.decodeFromString(response.bodyAsText())
        val xblToken = xblResponse.token
        val hash = xblResponse.displayClaims.xui.first().userHash
        return Pair(xblToken, hash)
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

        val response: HttpResponse = httpClient.post(XSTS_AUTH_URL) {
            contentType(ContentType.Application.Json)
            setBody(responseBody)
        }

        println(response.status)
        println("fn::authWithXSTS: ${response.bodyAsText()}")

        val xstsResponse: XstsTokenResponse = json.decodeFromString(response.bodyAsText())
        return xstsResponse.token
    }

    private suspend fun getMCToken(token: String, hash: String): String {
        val identity = "XBL3.0 x=$hash;$token"
        val body = mapOf("identityToken" to identity)

        val response: HttpResponse = httpClient.post("https://api.minecraftservices.com/authentication/login_with_xbox") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        println("MC Token Response: ${response.status} ${response.bodyAsText()}")

        if(response.status != HttpStatusCode.OK) {
            println("MC auth failed!")
            throw IllegalStateException("MC auth failed.")
        }

        val authResponse: MCAuthResponse = json.decodeFromString(response.bodyAsText())
        return authResponse.accessToken
    }

    private suspend fun fetchMCProfile(token: String): MCProfile? {
        val response: HttpResponse = httpClient.get(MC_PROFILE_URL) {
            header("Authorization", "Bearer $token")
        }

        val body = response.bodyAsText()
        println("Response status: ${response.status}")
        println("Response body: $body")

        return if (response.status == HttpStatusCode.OK) {
            json.decodeFromString(response.bodyAsText())
        } else {
            println("User does not own Minecraft, or other error.")
            null
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
    @SerialName("roles") val roles: Nothing? = null,
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