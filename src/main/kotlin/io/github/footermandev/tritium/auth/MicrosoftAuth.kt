package io.github.footermandev.tritium.auth

import com.microsoft.aad.msal4j.IAccount
import com.microsoft.aad.msal4j.InteractiveRequestParameters
import com.microsoft.aad.msal4j.SilentParameters
import io.github.footermandev.tritium.auth.MicrosoftAuth.isSignedIn
import io.github.footermandev.tritium.toURI
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Contains all logic for Minecraft authentication.
 *
 * @property isSignedIn Whether the user is signed in
 *
 * TODO: Clean up
 */
object MicrosoftAuth {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private var msalAcc: IAccount? = null

    suspend fun isSignedIn(): Boolean {
        return MSAL.app.accounts.await().isNotEmpty()
    }

    private suspend fun <T> CompletableFuture<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            this.whenComplete {res, err ->
                if(err != null) cont.resumeWithException(err) else cont.resume(res)
            }
        }

    private const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"

    suspend fun newSignIn(onSignedIn: (MCProfile?) -> Unit) {
        try {
            logger.info("Starting sign-in flow...")
            val scopes = setOf("XboxLive.signin", "offline_access")
            val parameters = InteractiveRequestParameters.builder("http://localhost".toURI())
                .scopes(scopes)
                .build()

            val result = MSAL.app.acquireToken(parameters).await()
            msalAcc = result.account()
            logger.info("MS token acquired; expires at ${result.expiresOnDate()}")

            val (xblToken, hash) = authWithLive(result.accessToken())
            val xstsToken = authWithXSTS(xblToken)
            val mcToken = getMCToken(xstsToken, hash)

            logger.info("Sign-in successful. MC token obtained.")

            ProfileMngr.Cache.init(mcToken)

            val profile = ProfileMngr.Cache.get()
            onSignedIn(profile)
        } catch (e: Exception) {
            logger.error("Sign-in failed: ${e.message}", e)
            signOut()
            throw e
        }
    }

    suspend fun ensureValidAccessToken(): String {
        val account = msalAcc ?: throw IllegalStateException("No signed-in account; sign in required.")
        return try {
            logger.info("Refreshing token silently...")
            val scopes = setOf("XboxLive.signin", "offline_access")
            val silentParams = SilentParameters.builder(scopes, account).build()
            val result = MSAL.app.acquireTokenSilently(silentParams).await()
            logger.info("Token refreshed silently; new expiry: ${result.expiresOnDate()}")
            result.accessToken()
        } catch (e: Exception) {
            logger.error("Silent token refresh failed: ${e.message}", e)
            signOut()
            throw e
        }
    }

    suspend fun getValidMCToken(): String {
        val msToken = ensureValidAccessToken()
        val (xblToken, hash) = authWithLive(msToken)
        val xstsToken = authWithXSTS(xblToken)
        return getMCToken(xstsToken, hash)
    }

    suspend fun getMCToken(msToken: String): String {
        val (xblToken, hash) = authWithLive(msToken)
        val xstsToken = authWithXSTS(xblToken)
        return getMCToken(xstsToken, hash)
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

    fun signOut() {
        runBlocking {
            MSAL.app.accounts.await().forEach { MSAL.app.removeAccount(it) }
        }
        msalAcc = null
        ProfileMngr.Cache.clear()
        logger.info("User signed out.")
    }

    suspend fun getMinecraftVersions(): List<MCVersion?>? {
        return try {
            val response = httpClient.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
            val body = response.bodyAsText()
            logger.info("Successfully fetched MC version manifest.")

            val manifest: VersionManifest = json.decodeFromString(body)

            val releases = manifest.versions.filter { it.type == "release" }
            logger.info("Filtered ${releases.size} release versions from the MC version manifest.")
            releases
        } catch (e: Exception) {
            logger.error("Error fetching Minecraft versions", e)
            null
        }
    }

    suspend fun downloadMinecraftVersion(version: MCVersion, destination: String): Boolean? {
        return try {
            logger.info("Downloading Minecraft '${version.id}' from ${version.url}")

            val content: ByteArray = httpClient.get(version.url).body()

            val file = File("$destination/${version.id}.jar")
            file.parentFile.mkdirs()
            file.writeBytes(content)
            logger.info("Downloaded Minecraft '${version.id}' to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            logger.error("Failed to download Minecraft ${version.id}", e)
            false
        }

    }
}

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
data class VersionManifest(
    val versions: List<MCVersion>
)

@Serializable
data class MCVersion(
    val id: String,
    val type: String,
    val url: String
)