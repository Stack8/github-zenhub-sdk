package zenhub

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.zenhub.graphql.sdk.*
import com.ziro.engineering.zenhub.graphql.sdk.type.*
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly
import java.time.Instant

/**
 * Default GitHub Repository ID - references the SMACS repository.
 */
const val DEFAULT_GITHUB_REPOSITORY_ID: Int = 15617306
const val DEFAULT_GIT_REPOSITORY_ID: String = "Z2lkOi8vcmFwdG9yL1JlcG9zaXRvcnkvMjEwNTg"

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

    private val apolloClient: ApolloClient = ApolloClient.Builder().serverUrl(ZENHUB_GRAPHQL_URL)
        .addHttpHeader("Authorization", "Bearer ${System.getenv("ZENHUB_GRAPHQL_TOKEN")}").build()

    fun searchClosedIssuesBetween(startTime: Instant, endTime: Instant): List<SearchClosedIssuesQuery.Node> {
        val results = ArrayList<SearchClosedIssuesQuery.Node>()
        val issueOnlyFilter = IssueSearchFiltersInput(
            displayType = Optional.present(DisplayFilter.issues),
        )
        var earliestClosedDate: Instant
        var cursor: String? = null

        do {
            val page = searchClosedIssues(issueOnlyFilter, cursor)
            page?.nodes?.let { results.addAll(it) }
            earliestClosedDate = Instant.parse(results.last().closedAt.toString())
            cursor = page?.pageInfo?.endCursor
        } while (earliestClosedDate.isAfter(startTime))

        return trimResults(results, startTime, endTime)
    }

    fun getCurrentSprint(): GetSprintsByStateQuery.Node? = runBlocking {
        val results = getSprintByState(
            SprintFiltersInput(Optional.present(SprintStateInput(SprintState.OPEN)), Optional.absent()),
            1,
            SprintOrderInput(Optional.present(OrderDirection.ASC), Optional.present(SprintOrderField.END_AT))
        )
        if (results.isNullOrEmpty()) {
            null
        } else {
            results[0]
        }
    }

    fun getPreviousSprint(): GetSprintsByStateQuery.Node? = runBlocking {
        val results = getSprintByState(
            SprintFiltersInput(Optional.present(SprintStateInput(SprintState.CLOSED)), Optional.absent()),
            1,
            SprintOrderInput(Optional.present(OrderDirection.DESC), Optional.present(SprintOrderField.END_AT))
        )
        if (results.isNullOrEmpty()) {
            null
        } else {
            results[0]
        }
    }

    fun issueByInfo(githubRepoId: Int, gitRepoId: String, issueNumber: Int): IssueByInfoQuery.IssueByInfo? = runBlocking {
        val query = IssueByInfoQuery(githubRepoId, gitRepoId, issueNumber)
        apolloClient.query(query).toFlow().single().data?.issueByInfo
    }

    fun addIssuesToSprints(
        issueIds: List<String>,
        sprintIds: List<String>
    ): AddIssuesToSprintsMutation.AddIssuesToSprints? = runBlocking {
        val input = AddIssuesToSprintsInput(Optional.absent(), issueIds, sprintIds)
        val mutation = AddIssuesToSprintsMutation(input)
        apolloClient.mutation(mutation).toFlow().single().data?.addIssuesToSprints
    }

    fun removeIssuesFromSprints(
        issueIds: List<String>,
        sprintIds: List<String>
    ): RemoveIssuesFromSprintsMutation.RemoveIssuesFromSprints? = runBlocking {
        val input = RemoveIssuesFromSprintsInput(Optional.absent(), issueIds, sprintIds)
        val mutation = RemoveIssuesFromSprintsMutation(input)
        apolloClient.mutation(mutation).toFlow().single().data?.removeIssuesFromSprints
    }

    fun getIssuesByPipeline(pipeline: Pipeline): List<GetIssuesByPipelineQuery.Node> = runBlocking {
        val query = GetIssuesByPipelineQuery(pipeline.id)
        apolloClient.query(query).toFlow().single().data?.searchIssuesByPipeline?.nodes ?: emptyList()
    }

    fun getReleases(githubRepoId: Int): List<GetReleasesQuery.Node> = runBlocking {
        val query = GetReleasesQuery(githubRepoId)
        apolloClient.query(query).toFlow().single().data?.repositoriesByGhId?.get(0)?.releases?.nodes
            ?: emptyList()
    }

    /**
     * Cannot move an issue to closed because closed is not a pipeline.
     */
    fun moveIssueToPipeline(issueId: String, pipelineId: String): MoveIssueMutation.MoveIssue? = runBlocking {
        val input = MoveIssueInput(Optional.absent(), pipelineId, issueId, Optional.present(0))
        val mutation = MoveIssueMutation(input, DEFAULT_WORKSPACE_ID)
        apolloClient.mutation(mutation).toFlow().single().data?.moveIssue
    }

    fun closeIssues(issueIds: List<String>): CloseIssuesMutation.CloseIssues? = runBlocking {
        val mutation = CloseIssuesMutation(issueIds)
        apolloClient.mutation(mutation).toFlow().single().data?.closeIssues
    }

    override fun close() {
        apolloClient.closeQuietly()
    }

    private fun getSprintByState(
        sprintFilters: SprintFiltersInput,
        firstSprints: Int,
        orderSprintsBy: SprintOrderInput
    ): List<GetSprintsByStateQuery.Node>? = runBlocking {
        val query = GetSprintsByStateQuery(zenhubWorkspaceId, sprintFilters, firstSprints, orderSprintsBy)
        apolloClient.query(query).toFlow().single().data?.workspace?.sprints?.nodes
    }

    private fun searchClosedIssues(filters: IssueSearchFiltersInput, after: String?): SearchClosedIssuesQuery.SearchClosedIssues? = runBlocking {
        val query = SearchClosedIssuesQuery(zenhubWorkspaceId, filters, Optional.present(100), Optional.presentIfNotNull(after))
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
