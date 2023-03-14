package zenhub.issue

internal data class ZenHubIssueQueryResult(val data: ZenHubIssueQueryData)

internal data class ZenHubIssueQueryData(val issues: ZenHubIssuePage)

internal data class ZenHubIssuePage(val nodes: List<ZenHubIssue>, val pageInfo: ZenHubPageInfo)

internal data class ZenHubIssue(
    val number: Int,
    val title: String,
    val pullRequest: Boolean,
    val closedAt: String,
    val user: ZenHubIssueUser,
    val assignees: ZenHubIssueAssignees
)

internal data class ZenHubIssueUser(val login: String)

internal data class ZenHubIssueAssignees(val nodes: List<ZenHubIssueUser>)

internal data class ZenHubPageInfo(val endCursor: String)
