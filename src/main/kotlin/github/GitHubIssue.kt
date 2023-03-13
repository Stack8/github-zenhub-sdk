package github

data class GitHubIssue(val number: Int, val title: String, val assignees: List<String>, val repository: String)
