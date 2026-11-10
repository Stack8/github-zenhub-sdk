package github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommitStatus(
    val state: String,
    val description: String?,
    @SerialName("target_url") val targetUrl: String?,
    val creator: SimpleUser?,
)

@Serializable data class SimpleUser(val login: String)
