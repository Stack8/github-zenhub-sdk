package zenhub

import com.ziro.engineering.zenhub.graphql.sdk.type.ReleaseState
import java.time.LocalDate

data class Release(
    val id: String,
    val title: String,
    val state: ReleaseState,
    val startOn: LocalDate,
    val endOn: LocalDate,
    val issueIds: Set<String>
)
