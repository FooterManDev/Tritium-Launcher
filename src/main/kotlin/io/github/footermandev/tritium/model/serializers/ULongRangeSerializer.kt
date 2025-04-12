package io.github.footermandev.tritium.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ULongRangeSerializer : KSerializer<ULongRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ULongRange") {
        element("start", ULong.serializer().descriptor)
        element("endInclusive", ULong.serializer().descriptor)
    }
    override fun serialize(encoder: Encoder, value: ULongRange) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, ULong.serializer(), value.start)
        composite.encodeSerializableElement(descriptor, 1, ULong.serializer(), value.endInclusive)
        composite.endStructure(descriptor)
    }
    override fun deserialize(decoder: Decoder): ULongRange {
        val dec = decoder.beginStructure(descriptor)
        var start = 0uL
        var endInclusive = 0uL
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> start = dec.decodeSerializableElement(descriptor, 0, ULong.serializer())
                1 -> endInclusive = dec.decodeSerializableElement(descriptor, 1, ULong.serializer())
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index: $index")
            }
        }
        dec.endStructure(descriptor)
        return start..endInclusive
    }
}