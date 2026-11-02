package com.efina.app.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/**
 * Accepts `true`/`false`, `"true"`/`"false"`, and D1's `0`/`1`.
 *
 * The backend stores these flags as SQLite integers, so
 * `SELECT ... has_seen_tutorial` and `POST auth/register` (which hardcodes 0)
 * both emit numbers. kotlinx-serialization's built-in lenient boolean mode only
 * accepts the literal tokens `true` and `false`, so a plain `Boolean` property
 * threw on every response that carried a 0 or 1 — which made registration look
 * like it had failed even though the server had created the account.
 */
object LenientBoolean : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val json = decoder as? JsonDecoder
            ?: return decoder.decodeBoolean()
        val primitive = json.decodeJsonElement() as? JsonPrimitive
            ?: return false
        return parse(primitive)
    }

    internal fun parse(primitive: JsonPrimitive): Boolean {
        if (primitive is JsonUnquotedLiteral) return primitive.content != "0"
        primitive.booleanOrNull?.let { return it }
        val raw = primitive.content
        return when {
            raw.equals("true", ignoreCase = true) -> true
            raw.equals("false", ignoreCase = true) -> false
            else -> raw.toLongOrNull()?.let { it != 0L } ?: false
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}
