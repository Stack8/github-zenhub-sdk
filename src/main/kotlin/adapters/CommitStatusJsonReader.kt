package adapters

import com.apollographql.apollo3.api.json.JsonReader
import com.ziro.engineering.github.graphql.sdk.type.StatusState
import github.CommitStatus
import java.net.URI
import java.time.Instant

internal fun JsonReader.readCommitStatuses(): List<CommitStatus> {
    val statuses = mutableListOf<CommitStatus>()
    beginArray()

    while (hasNext()) {
        statuses.add(readCommitStatus())
    }

    endArray()
    return statuses
}

private fun JsonReader.readCommitStatus(): CommitStatus {
    var context: String? = null
    var state: StatusState? = null
    var description: String? = null
    var targetUrl: URI? = null
    var creatorLogin: String? = null
    var createdAt: Instant? = null
    beginObject()

    while (hasNext()) {
        when (nextName()) {
            "context" -> context = nextNullableString()
            "state" -> state = nextNullableString()?.uppercase()?.let(StatusState::safeValueOf)
            "description" -> description = nextNullableString()
            "target_url" -> targetUrl = nextNullableString()?.let(::URI)
            "creator" -> creatorLogin = readCreatorLogin()
            "created_at" -> createdAt = nextNullableString()?.let(Instant::parse)
            else -> skipValue()
        }
    }

    endObject()

    return CommitStatus(
        context = requireNotNull(context) { "Commit status has no context" },
        state = requireNotNull(state) { "Commit status has no state" },
        description = description,
        targetUrl = targetUrl,
        creatorLogin = creatorLogin,
        createdAt = requireNotNull(createdAt) { "Commit status has no creation time" },
    )
}

private fun JsonReader.readCreatorLogin(): String? {
    if (peek() == JsonReader.Token.NULL) {
        return nextNull()
    }

    var login: String? = null
    beginObject()

    while (hasNext()) {
        if (nextName() == "login") {
            login = nextNullableString()
        } else {
            skipValue()
        }
    }

    endObject()
    return login
}

private fun JsonReader.nextNullableString(): String? =
    if (peek() == JsonReader.Token.NULL) nextNull() else nextString()
