package zenhub.issue

data class ZenHubIssueSearchResult(
    val number: Int,
    val title: String,
    val pullRequest: Boolean,
    val author: String,
    val assignees: List<String>,
    val labels: List<String>
)
