package shared

import kotlinx.serialization.Serializable

@Serializable
data class GraphQuery(val query: String)