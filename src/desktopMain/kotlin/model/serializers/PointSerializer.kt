package model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Point

/**
 * Custom serializer for [Point]
 */
class PointSerializer : KSerializer<Point> {
    override val descriptor = PrimitiveSerialDescriptor("Point", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Point {
        val (x, y) = decoder.decodeString().split(",").map { it.toInt() }
        return Point(x, y)
    }

    override fun serialize(encoder: Encoder, value: Point) {
        encoder.encodeString("${value.x},${value.y}")
    }
}