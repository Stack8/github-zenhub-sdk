package adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.NullableAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import java.net.URI

class UriAdapter : Adapter<URI> {

    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): URI {
        return URI(reader.nextString()!!)
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: URI
    ) {
        writer.value(value.toString())
    }

    /** Required by the generated SDK to handle URI? i.e. nullable URI */
    companion object {
        fun nullable(): NullableAdapter<URI> {
            return NullableAdapter(wrappedAdapter = UriAdapter())
        }

        fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): URI {
            return URI(reader.nextString()!!)
        }

        fun toJson(
            writer: JsonWriter,
            customScalarAdapters: CustomScalarAdapters,
            value: URI
        ) {
            writer.value(value.toString())
        }
    }
}
