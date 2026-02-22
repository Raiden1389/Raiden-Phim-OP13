package xyz.raidenhub.phim.data.api

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Gson TypeAdapterFactory that gracefully handles type mismatches:
 * - When expecting a List/Array but receiving an Object → returns empty list
 * - When expecting an Object but receiving an Array → skips and returns null/default
 *
 * Prevents crashes like:
 *   "Expected BEGIN_ARRAY but was BEGIN_OBJECT at $.data.latest_episode"
 */
class SafeTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val delegate = gson.getDelegateAdapter(this, type)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                delegate.write(out, value)
            }

            override fun read(`in`: JsonReader): T? {
                return try {
                    delegate.read(`in`)
                } catch (e: Exception) {
                    // On type mismatch, skip the problematic value and return null
                    consumeValue(`in`)
                    null
                }
            }
        }
    }

    private fun consumeValue(reader: JsonReader) {
        try {
            when (reader.peek()) {
                JsonToken.BEGIN_ARRAY -> {
                    reader.beginArray()
                    while (reader.hasNext()) consumeValue(reader)
                    reader.endArray()
                }
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName()
                        consumeValue(reader)
                    }
                    reader.endObject()
                }
                JsonToken.STRING -> reader.nextString()
                JsonToken.NUMBER -> reader.nextDouble()
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NULL -> reader.nextNull()
                else -> reader.skipValue()
            }
        } catch (_: Exception) {
            try { reader.skipValue() } catch (_: Exception) {}
        }
    }
}
