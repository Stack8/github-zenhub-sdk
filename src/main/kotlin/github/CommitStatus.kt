package github

import com.ziro.engineering.github.graphql.sdk.type.StatusState
import java.net.URI
import java.time.Instant

data class CommitStatus(
    val context: String,
    val state: StatusState,
    val description: String?,
    val targetUrl: URI?,
    val creatorLogin: String?,
    val createdAt: Instant,
)
