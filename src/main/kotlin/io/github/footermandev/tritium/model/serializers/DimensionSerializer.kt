package io.github.footermandev.tritium.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Dimension

/**
 * Custom serializer for [Dimension]
 */
class DimensionSerializer : KSerializer<Dimension> {
    override val descriptor = PrimitiveSerialDescriptor("Dimension", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Dimension {
        val (width, height) = decoder.decodeString().split(",").map { it.toInt() }
        return Dimension(width, height)
    }

    override fun serialize(encoder: Encoder, value: Dimension) {
        encoder.encodeString("${value.width},${value.height}")
    }

}