package adapters

import com.apollographql.apollo3.api.json.JsonWriter


/** Writes plain [json] into this [JsonWriter]. */
fun JsonWriter.writeJson(json: String) {
    jsonReader(json).writeInto(writer = this)
}
