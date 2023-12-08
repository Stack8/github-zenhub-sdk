package zenhub

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.zenhub.graphql.sdk.IssueByInfoQuery
import com.ziro.engineering.zenhub.graphql.sdk.SearchClosedIssuesQuery
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly
import java.time.Instant

class ZenHubClient : AutoCloseable {

    private val apolloClient: ApolloClient = ApolloClient.Builder().serverUrl("https://api.zenhub.com/public/graphql")
        .addHttpHeader("Authorization", "Bearer ${System.getenv("ZENHUB_GRAPHQL_TOKEN")}")
        .build()

    object Constants {
        const val GITHUB_REPOSITORY_ID: Int = 15617306
        const val GIT_REPOSITORY_ID: String = "Z2lkOi8vcmFwdG9yL1JlcG9zaXRvcnkvMjEwNTg"
        const val ZENHUB_WORKSPACE_ID: String = "59c54eb49d9e774e473597f1"
    }

    fun searchClosedIssuesBetween(startTime: Instant, endTime: Instant): List<SearchClosedIssuesQuery.Node> {
        var results = emptyList<SearchClosedIssuesQuery.Node>()
        var earliestClosedDate: Instant
        var cursor: String? = null

        do {
            val page = searchClosedIssues(cursor)
            results.plus(page?.nodes)
            earliestClosedDate = Instant.parse(page?.nodes?.last()?.closedAt.toString())
            cursor = page?.pageInfo?.endCursor
            val nodes: List<SearchClosedIssuesQuery.Node>? = page?.nodes
            nodes?.forEach { results = results.plus(it) }
        } while (earliestClosedDate.isAfter(startTime))

        return trimResults(results, startTime, endTime)
    }

    fun issueByInfo(issueNumber: Int): IssueByInfoQuery.IssueByInfo? = runBlocking {
        val query = IssueByInfoQuery(Constants.GITHUB_REPOSITORY_ID, Constants.GIT_REPOSITORY_ID, issueNumber)
        apolloClient.query(query).toFlow().single().data?.issueByInfo
    }

    override fun close() {
        apolloClient.closeQuietly()
    }

    private fun searchClosedIssues(after: String?): SearchClosedIssuesQuery.SearchClosedIssues? = runBlocking {
        val query = SearchClosedIssuesQuery(Constants.ZENHUB_WORKSPACE_ID, 100, Optional.presentIfNotNull(after))
        apolloClient.query(query).toFlow().single().data?.searchClosedIssues
    }

    private fun trimResults(results: List<SearchClosedIssuesQuery.Node>, startDate: Instant, endDate: Instant): List<SearchClosedIssuesQuery.Node> {
        // The results are returned in reverse chronological order.
        if (results.isEmpty()) {
            return results
        }

        val indexOfEarliestIssue = results.indexOfFirst { issue -> Instant.parse(issue.closedAt.toString()).isBefore(startDate) }

        var indexOfLatestIssue = -1
        if (Instant.parse(results[0].closedAt.toString()).isAfter(endDate)) {
            indexOfLatestIssue = results.indexOfLast { issue -> Instant.parse(issue.closedAt.toString()).isAfter(endDate) }
        }

        return results.subList(indexOfLatestIssue + 1, indexOfEarliestIssue)
    }

}
