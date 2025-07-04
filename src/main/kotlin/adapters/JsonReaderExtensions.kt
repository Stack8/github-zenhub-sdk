package adapters

import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.exception.JsonEncodingException
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8

/**
 * Gets contents of this [JsonReader] as UTF-8 string.
 *
 * @throws IllegalStateException wrong reader / writer state.
 * @throws NotImplementedError [JsonReader.Token.ANY] token is not implemented.
 */
fun JsonReader.readAsUtf8String(): String {
    return Buffer().also { BufferedSinkJsonWriter(it).run(::writeInto) }.readUtf8()
}

/**
 * Writes contents of this [JsonReader] into [writer].
 *
 * @throws JsonEncodingException invalid JSON.
 * @throws JsonEncodingException wrong reader / writer state.
 * @throws NotImplementedError [JsonReader.Token.ANY] token is not implemented.
 */
fun JsonReader.writeInto(writer: JsonWriter) {
    val reader = this
    var depth = 0

    while (reader.hasNext() ||
        (reader.peek() == JsonReader.Token.END_ARRAY ||
            reader.peek() == JsonReader.Token.END_OBJECT)) {
        // hasNext() returns false when peek() returns END_OBJECT or END_ARRAY token

        val token = reader.peek()

        when (token) {
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                writer.beginArray()
                depth++
            }

            JsonReader.Token.END_ARRAY -> {
                reader.endArray()
                writer.endArray()
                depth--

                if (depth == 0) break
            }

            JsonReader.Token.BEGIN_OBJECT -> {
                reader.beginObject()
                writer.beginObject()
                depth++
            }

            JsonReader.Token.END_OBJECT -> {
                reader.endObject()
                writer.endObject()
                depth--

                if (depth == 0) break
            }

            JsonReader.Token.NAME -> {
                val name = reader.nextName()
                writer.name(name)
            }

            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                writer.value(value.orEmpty())

                if (depth == 0) break
            }

            JsonReader.Token.NUMBER -> {
                val value = reader.nextNumber()
                writer.value(value)

                if (depth == 0) break
            }

            JsonReader.Token.LONG -> {
                val value = reader.nextLong()
                writer.value(value)

                if (depth == 0) break
            }

            JsonReader.Token.BOOLEAN -> {
                val value = reader.nextBoolean()
                writer.value(value)

                if (depth == 0) break
            }

            JsonReader.Token.NULL -> {
                reader.nextNull()
                writer.nullValue()

                if (depth == 0) break
            }

            JsonReader.Token.END_DOCUMENT -> {
                break
            }

            JsonReader.Token.ANY -> throw NotImplementedError("ANY token not supported.")
        }
    }

    // Check that the reader and writer are at the same depth
    check(depth == 0) { "Reader and writer depth mismatch: $depth" }
}

/** Creates new [JsonReader] from this plain [json]. */
internal fun jsonReader(json: String): JsonReader {
    return BufferedSourceJsonReader(Buffer().also { it.write(json.encodeUtf8()) })
}
