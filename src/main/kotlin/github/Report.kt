package github

import github.model.GitHubIssueSearchQuery
import github.model.IssueStatus
import github.model.IssueType

suspend fun main() {
    val searchGitHubIssues = searchGitHubIssues(
        GitHubIssueSearchQuery("Stack8", IssueType.ISSUE).issueStatus(IssueStatus.CLOSED)
            .closedOn("2023-02-01..2023-03-01")
    )
    println(searchGitHubIssues.count())
    println(searchGitHubIssues.get(0).toString())
}