package io.github.footermandev.tritium.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LongRangeSerializer : KSerializer<LongRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LongRange") {
        element<Long>("start")
        element<Long>("endInclusive")
    }
    override fun serialize(encoder: Encoder, value: LongRange) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeLongElement(descriptor, 0, value.first)
        composite.encodeLongElement(descriptor, 1, value.last)
        composite.endStructure(descriptor)
    }
    override fun deserialize(decoder: Decoder): LongRange {
        val dec = decoder.beginStructure(descriptor)
        var start = 0L
        var endInclusive = 0L
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> start = dec.decodeLongElement(descriptor, 0)
                1 -> endInclusive = dec.decodeLongElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index: $index")
            }
        }
        dec.endStructure(descriptor)
        return start..endInclusive
    }
}