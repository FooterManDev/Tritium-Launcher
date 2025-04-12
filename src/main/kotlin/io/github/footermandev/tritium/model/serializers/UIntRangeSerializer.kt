package io.github.footermandev.tritium.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UIntRangeSerializer : KSerializer<UIntRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UIntRange") {
        element("start", UInt.serializer().descriptor)
        element("endInclusive", UInt.serializer().descriptor)
    }
    override fun serialize(encoder: Encoder, value: UIntRange) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, UInt.serializer(), value.start)
        composite.encodeSerializableElement(descriptor, 1, UInt.serializer(), value.endInclusive)
        composite.endStructure(descriptor)
    }
    override fun deserialize(decoder: Decoder): UIntRange {
        val dec = decoder.beginStructure(descriptor)
        var start = 0u
        var endInclusive = 0u
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> start = dec.decodeSerializableElement(descriptor, 0, UInt.serializer())
                1 -> endInclusive = dec.decodeSerializableElement(descriptor, 1, UInt.serializer())
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index: $index")
            }
        }
        dec.endStructure(descriptor)
        return start..endInclusive
    }
}