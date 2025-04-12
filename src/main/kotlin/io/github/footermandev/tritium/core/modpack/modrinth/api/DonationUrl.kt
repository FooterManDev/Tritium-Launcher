package io.github.footermandev.tritium.core.modpack.modrinth.api

import kotlinx.serialization.Serializable

@Serializable
data class DonationUrl(
    val id: String,
    val platform: String,
    val url: String
)