package zenhub.issue

import kotlinx.serialization.Serializable

@Serializable
internal data class ZenHubIssueQueryResult(val data: ZenHubIssueQueryData)

@Serializable
internal data class ZenHubIssueQueryData(val issues: ZenHubIssuePage)

@Serializable
internal data class ZenHubIssuePage(val nodes: List<ZenHubIssue>, val pageInfo: ZenHubPageInfo)

@Serializable
internal data class ZenHubIssue(
    val number: Int,
    val title: String,
    val pullRequest: Boolean,
    val closedAt: String,
    val user: ZenHubIssueUser,
    val assignees: ZenHubIssueAssignees
)

@Serializable
internal data class ZenHubIssueUser(val login: String)

@Serializable
internal data class ZenHubIssueAssignees(val nodes: List<ZenHubIssueUser>)

@Serializable
internal data class ZenHubPageInfo(val endCursor: String)
