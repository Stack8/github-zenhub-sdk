package zenhub

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.zenhub.graphql.sdk.IssueByInfoQuery
import com.ziro.engineering.zenhub.graphql.sdk.SearchClosedIssuesQuery
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly
import java.time.Instant

/**
 * Default GitHub Repository ID - references the SMACS repository.
 */
private const val DEFAULT_GITHUB_REPOSITORY_ID: Int = 15617306
private const val DEFAULT_GIT_REPOSITORY_ID: String = "Z2lkOi8vcmFwdG9yL1JlcG9zaXRvcnkvMjEwNTg"

/**
 * Default Workspace ID - references the "Engineering Team" workspace.
 */
private const val DEFAULT_WORKSPACE_ID = "59c54eb49d9e774e473597f1"
private const val ZENHUB_GRAPHQL_URL = "https://api.zenhub.com/public/graphql"

class ZenHubClient(
    private val githubRepositoryId: Int = DEFAULT_GITHUB_REPOSITORY_ID,
    private val gitRepositoryId: String = DEFAULT_GIT_REPOSITORY_ID,
    private val zenhubWorkspaceId: String = DEFAULT_WORKSPACE_ID
) : AutoCloseable {

    private val apolloClient: ApolloClient = ApolloClient
        .Builder()
        .serverUrl(ZENHUB_GRAPHQL_URL)
        .addHttpHeader(
            "Authorization",
            "Bearer ${System.getenv("ZENHUB_GRAPHQL_TOKEN")}"
        )
        .build()

    fun searchClosedIssuesBetween(
        startTime: Instant,
        endTime: Instant
    ): List<SearchClosedIssuesQuery.Node> {
        val results = ArrayList<SearchClosedIssuesQuery.Node>()
        var earliestClosedDate: Instant
        var cursor: String? = null

        do {
            val page = searchClosedIssues(cursor)
            page?.nodes?.let { results.addAll(it) }
            earliestClosedDate = Instant.parse(results.last().closedAt.toString())
            cursor = page?.pageInfo?.endCursor
        } while (earliestClosedDate.isAfter(startTime))

        return trimResults(results, startTime, endTime)
    }

    fun issueByInfo(issueNumber: Int): IssueByInfoQuery.IssueByInfo? = runBlocking {
        val query = IssueByInfoQuery(githubRepositoryId, gitRepositoryId, issueNumber)
        apolloClient.query(query).toFlow().single().data?.issueByInfo
    }

    override fun close() {
        apolloClient.closeQuietly()
    }

    private fun searchClosedIssues(after: String?): SearchClosedIssuesQuery.SearchClosedIssues? =
        runBlocking {
            val query =
                SearchClosedIssuesQuery(
                    zenhubWorkspaceId,
                    Optional.present(100),
                    Optional.presentIfNotNull(after)
                )
            apolloClient.query(query).toFlow().single().data?.searchClosedIssues
        }

    private fun trimResults(
        results: List<SearchClosedIssuesQuery.Node>,
        startDate: Instant,
        endDate: Instant
    ): List<SearchClosedIssuesQuery.Node> {
        // The results are returned in reverse chronological order.
        if (results.isEmpty()) {
            return results
        }

        val indexOfEarliestIssue = results.indexOfFirst { issue ->
            Instant.parse(issue.closedAt.toString()).isBefore(startDate)
        }

        var indexOfLatestIssue = -1
        if (Instant.parse(results[0].closedAt.toString()).isAfter(endDate)) {
            indexOfLatestIssue = results.indexOfLast { issue ->
                Instant.parse(issue.closedAt.toString()).isAfter(endDate)
            }
        }

        return results.subList(indexOfLatestIssue + 1, indexOfEarliestIssue)
    }

}
