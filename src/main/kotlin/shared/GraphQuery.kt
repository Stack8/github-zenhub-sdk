package shared

import kotlinx.serialization.Serializable

@Serializable
internal data class GraphQuery(val query: String)
