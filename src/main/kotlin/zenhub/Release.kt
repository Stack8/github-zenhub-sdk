package zenhub

data class Release(
    val issues: List<Issue>
) {
    data class Issue(
        val id: String,
        val title: String,
        val number: Int
    )
}
