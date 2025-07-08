package adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.NullableAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class UriAdapter : Adapter<String> {

    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
        return reader.nextString()!!
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: String
    ) {
        writer.value(value)
    }

    /** Required by the generated SDK to handle URI? i.e. nullable URI */
    companion object {
        fun nullable(): NullableAdapter<String> {
            return NullableAdapter(wrappedAdapter = UriAdapter())
        }
    }
}
