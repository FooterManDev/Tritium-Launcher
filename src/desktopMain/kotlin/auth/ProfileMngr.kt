package auth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import java.io.File

@Serializable
data class MCProfile(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("skins") val skins: List<MCSkin>,
    @SerialName("capes") val capes: List<MCCape>
)
@Serializable
data class MCSkin(
    val id: String,
    val state: String,
    val url: String,
    val variant: String
)
@Serializable
data class MCCape(
    val id: String,
    val state: String,
    val url: String,
    val alias: String
)

/**
 * Holds methods for profile management, and the profile cache.
 */
object ProfileMngr {
    private const val MC_API_BASE = "https://api.minecraftservices.com"
    private const val PROFILE_URL = "$MC_API_BASE/minecraft/profile"

    private const val SKIN_CHANGE_URL = "$MC_API_BASE/minecraft/profile/skins"
    private const val CAPE_CHANGE_URL = "$MC_API_BASE/minecraft/profile/capes/active"

    private val listeners = mutableListOf<(MCProfile?) -> Unit>()

    fun addListener(listener: (MCProfile?) -> Unit) {
        listeners.add(listener)
    }

    private fun notifyProfileChanged(profile: MCProfile?) {
        listeners.forEach { it(profile) }
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val logger = LoggerFactory.getLogger(this::class.java)

    object Cache {
        private var msToken: String? = null
        private var cachedProfile: MCProfile? = null

        suspend fun init(token: String) = withContext(Dispatchers.IO) {
            msToken = token
            try {
                val profile = fetch(token)
                if (profile != null) {
                    cachedProfile = profile
                    logger.info("Profile fetched and cached: ${profile.name}")
                } else logger.error("Failed to fetch MC profile during initialization.")
            } catch (e: Exception) {
                logger.error("Exception while fetching MC profile: ${e.message}", e)
            }
            notifyProfileChanged(cachedProfile)
        }

        suspend fun get(): MCProfile? = withContext(Dispatchers.IO) { cachedProfile }

        fun getUsername(): String? = cachedProfile?.name
        fun getUUID(): String? = cachedProfile?.id
        fun getSkins(): List<MCSkin>? = cachedProfile?.skins
        fun getCapes(): List<MCCape>? = cachedProfile?.capes

        fun clear() {
            msToken = null
            cachedProfile = null
            logger.info("Profile cache cleared.")
            notifyProfileChanged(null)
        }
    }

    suspend fun fetch(token: String): MCProfile? {
        return try {
            val response: HttpResponse = httpClient.get(PROFILE_URL) {
                header("Authorization", "Bearer $token")
            }
            if (response.status != HttpStatusCode.OK) {
                logger.error("Failed to fetch profile: HTTP ${response.status.value}")
                null
            } else {
                json.decodeFromString(MCProfile.serializer(), response.bodyAsText())
            }
        } catch (e: Exception) {
            logger.error("Error fetching MC profile: ${e.message}", e)
            null
        }
    }

    suspend fun changeName(token: String, newName: String): Boolean {
        val url = "$PROFILE_URL/name/$newName"
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.put(url) {
                    headers {
                        append("Authorization", "Bearer $token")
                        append("Content-Length", "0")
                    }
                }
                when (response.status) {
                    HttpStatusCode.NoContent -> true
                    HttpStatusCode.BadRequest -> {
                        logger.error("Invalid profile name: $newName")
                        false
                    }

                    HttpStatusCode.Forbidden -> {
                        logger.error("Could not change name for profile")
                        false
                    }

                    HttpStatusCode.TooManyRequests -> {
                        logger.error("Too many requests sent")
                        false
                    }

                    else -> {
                        logger.error("Error changing username: ${response.status.value}")
                        false
                    }
                }
            } catch (e: Exception) {
                logger.error("Error changing username: ${e.message}", e)
                false
            }
        }
    }

    suspend fun changeSkin(token: String, skinId: String, variant: String = "classic"): Boolean {
        return try {
            val response = httpClient.put("$SKIN_CHANGE_URL/$skinId") {
                headers { append("Authorization", "Bearer $token") }
                contentType(ContentType.Application.Json)
                setBody(JsonObject(mapOf("variant" to JsonPrimitive(variant))))
            }
            if (response.status == HttpStatusCode.NoContent) {
                logger.info("Skin changed successfully to skin id $skinId")
                true
            } else {
                logger.error("Failed to change skin: HTTP ${response.status.value}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error changing skin: ${e.message}", e)
            false
        }
    }

    suspend fun uploadSkin(token: String, file: File, variant: String = "classic"): Boolean {
        return try {
            val response = httpClient.post(SKIN_CHANGE_URL) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.MultiPart.FormData)
                setBody(MultiPartFormDataContent(formData {
                    append("variant", variant)
                    append("file", file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"${file.name}\"")
                    })
                }))
            }
            if (response.status == HttpStatusCode.NoContent) {
                logger.info("Skin uploaded successfully: ${file.name}")
                true
            } else {
                logger.error("Failed to upload skin: HTTP ${response.status.value}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error uploading skin: ${e.message}", e)
            false
        }
    }

    suspend fun changeCape(token: String, capeId: String): Boolean {
        return try {
            val response = httpClient.put("$CAPE_CHANGE_URL/$capeId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.NoContent) {
                logger.info("Cape changed successfully to cape id $capeId")
                true
            } else {
                logger.error("Failed to change cape: HTTP ${response.status.value}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error changing cape: ${e.message}", e)
            false
        }
    }
}