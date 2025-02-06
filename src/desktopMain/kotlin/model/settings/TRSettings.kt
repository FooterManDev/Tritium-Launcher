package model.settings

import kotlinx.serialization.Serializable

/**
 * The main settings data class.
 * TODO: I'm not 100% on this being the main settings system.
 */
@Serializable
data class TRSettings(
    val ver: Int = 0, // Settings version
    val java: Java = Java(null, null, null, null)
) {
    /**
     * Java version paths.
     */
    @Serializable
    data class Java(
        val j8Path: String? = null,
        val j16Path: String? = null,
        val j17Path: String? = null,
        val j21Path: String? = null
    )
}
