package zenhub

data class EpicData(
    val id: String,
    val epicIssueId: String,
    val childIssuesIds: Set<String>
)
