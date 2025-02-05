package adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.NullableAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

/**
 * Adapter for JSON custom scalar to its String representation
 */
class JsonAdapter : Adapter<String> {

    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
        return reader.readAsUtf8String()
    }

    override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
        writer.writeJson(value)
    }

    /**
     * Required by the generated SDK to handle JSON? i.e. nullable JSON
     */
    companion object {
        fun nullable(): NullableAdapter<String> {
            return NullableAdapter(
                wrappedAdapter = JsonAdapter()
            )
        }
    }
}
