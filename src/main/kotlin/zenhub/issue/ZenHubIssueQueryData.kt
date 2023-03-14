package zenhub.issue

data class ZenHubIssueQueryResult(val data: ZenHubIssueQueryData)

data class ZenHubIssueQueryData(val issues: ZenHubIssuePage)

data class ZenHubIssuePage(val nodes: List<ZenHubIssue>, val pageInfo: ZenHubPageInfo)

data class ZenHubIssue(
    val number: Int,
    val title: String,
    val pullRequest: Boolean,
    val closedAt: String,
    val user: ZenHubIssueUser,
    val assignees: ZenHubIssueAssignees
)

data class ZenHubIssueUser(val login: String)

data class ZenHubIssueAssignees(val nodes: List<ZenHubIssueUser>)

data class ZenHubPageInfo(val endCursor: String)
