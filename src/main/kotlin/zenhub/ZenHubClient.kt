package zenhub

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.zenhub.graphql.sdk.*
import com.ziro.engineering.zenhub.graphql.sdk.type.*
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly

/** Default GitHub Repository ID - references the SMACS repository. */
const val DEFAULT_GITHUB_REPOSITORY_ID: Int = 15617306
const val DEFAULT_GIT_REPOSITORY_ID: String = "Z2lkOi8vcmFwdG9yL1JlcG9zaXRvcnkvMjEwNTg"

/** Default Workspace ID - references the "Engineering Team" workspace. */
private const val DEFAULT_WORKSPACE_ID = "59c54eb49d9e774e473597f1"
private const val ZENHUB_GRAPHQL_URL = "https://api.zenhub.com/public/graphql"

private const val DEFAULT_PAGE_SIZE = 100

class ZenHubClient(
    private val githubRepositoryId: Int = DEFAULT_GITHUB_REPOSITORY_ID,
    private val gitRepositoryId: String = DEFAULT_GIT_REPOSITORY_ID,
    private val zenhubWorkspaceId: String = DEFAULT_WORKSPACE_ID
) : AutoCloseable {

    private val apolloClient: ApolloClient =
        ApolloClient.Builder()
            .serverUrl(ZENHUB_GRAPHQL_URL)
            .addHttpHeader("Authorization", "Bearer ${System.getenv("ZENHUB_GRAPHQL_TOKEN")}")
            .build()

    fun searchClosedIssuesBetween(
        startTime: Instant,
        endTime: Instant
    ): List<SearchClosedIssuesQuery.Node> {
        val results = ArrayList<SearchClosedIssuesQuery.Node>()
        val issueOnlyFilter =
            IssueSearchFiltersInput(
                displayType = Optional.present(DisplayFilter.issues),
            )
        var earliestClosedDate: Instant
        var cursor: String? = null

        do {
            val page = searchClosedIssues(issueOnlyFilter, cursor)
            page?.nodes?.let { results.addAll(it) }
            earliestClosedDate = Instant.parse(results.last().issueFragment.closedAt.toString())
            cursor = page?.pageInfo?.endCursor
        } while (earliestClosedDate.isAfter(startTime))

        return trimResults(results, startTime, endTime)
    }

    fun getCurrentSprint(): GetSprintsByStateQuery.Node? = runBlocking {
        val results =
            getSprintByState(
                SprintFiltersInput(
                    Optional.present(SprintStateInput(SprintState.OPEN)), Optional.absent()),
                SprintOrderInput(
                    Optional.present(OrderDirection.ASC),
                    Optional.present(SprintOrderField.END_AT)))

        if (results.isEmpty()) {
            null
        } else {
            results[0]
        }
    }

    fun getPreviousSprint(): GetSprintsByStateQuery.Node? = runBlocking {
        val results =
            getSprintByState(
                SprintFiltersInput(
                    Optional.present(SprintStateInput(SprintState.CLOSED)), Optional.absent()),
                SprintOrderInput(
                    Optional.present(OrderDirection.DESC),
                    Optional.present(SprintOrderField.END_AT)))

        if (results.isEmpty()) {
            null
        } else {
            results[0]
        }
    }

    fun issueByInfo(
        githubRepoId: Int,
        gitRepoId: String,
        issueNumber: Int
    ): IssueByInfoQuery.IssueByInfo? = runBlocking {
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
        val issues: ArrayList<GetIssuesByPipelineQuery.Node> = ArrayList()
        var hasNextPage: Boolean
        var endCursor: String? = null

        do {
            val issuesQuery =
                GetIssuesByPipelineQuery(pipeline.id, Optional.presentIfNotNull(endCursor))
            val issuesInPage =
                apolloClient.query(issuesQuery).toFlow().single().data?.searchIssuesByPipeline

            if (issuesInPage?.nodes != null) {
                issues.addAll(issuesInPage.nodes)
            }

            hasNextPage = issuesInPage?.pageInfo?.hasNextPage ?: false
            endCursor = issuesInPage?.pageInfo?.endCursor
        } while (hasNextPage)

        issues
    }

    fun getReleases(githubRepoId: Int, includeIssues: Boolean = true): Set<Release> = runBlocking {
        val allReleases: ArrayList<GetReleasesQuery.Node> = ArrayList()
        val releaseIdToIssueIdsMap = mutableMapOf<String, MutableSet<String>>()
        var releases: GetReleasesQuery.Releases?
        var hasNextReleasePage = false
        var releasesEndCursor: String? = null

        do {
            val releasesQuery =
                GetReleasesQuery(githubRepoId, Optional.presentIfNotNull(releasesEndCursor))

            releases =
                apolloClient
                    .query(releasesQuery)
                    .toFlow()
                    .single()
                    .data
                    ?.repositoriesByGhId
                    ?.get(0)
                    ?.releases

            if (includeIssues) {
                queryIssues(releases, releaseIdToIssueIdsMap)
            }

            if (releases != null) {
                hasNextReleasePage = releases.pageInfo.hasNextPage
                releasesEndCursor = releases.pageInfo.endCursor
                allReleases.addAll(releases.nodes)
            }
        } while (hasNextReleasePage)

        allReleases
            .map { release ->
                Release(
                    release.id,
                    release.title,
                    release.state,
                    LocalDate.parse(release.startOn.toString()),
                    LocalDate.parse(release.endOn.toString()),
                    releaseIdToIssueIdsMap[release.id]?.toSet() ?: emptySet(),
                )
            }
            .toSet()
    }

    private fun queryIssues(
        releases: GetReleasesQuery.Releases?,
        releaseIdToIssueIdsMap: MutableMap<String, MutableSet<String>>
    ) = runBlocking {
        releases?.nodes?.forEach { release ->
            val issuesCollectedSoFar = release.issues.nodes.map { issue -> issue.id }.toMutableSet()
            var hasNextIssuePage = release.issues.pageInfo.hasNextPage
            var issuesEndCursor = release.issues.pageInfo.endCursor
            var issues: GetReleaseQuery.Issues?

            while (hasNextIssuePage) {
                val releaseQuery =
                    GetReleaseQuery(release.id, Optional.presentIfNotNull(issuesEndCursor))

                issues =
                    apolloClient.query(releaseQuery).toFlow().single().data?.node?.onRelease?.issues

                if (issues?.nodes != null) {
                    issuesCollectedSoFar.addAll(issues.nodes.map { issue -> issue.id })
                }

                if (issues != null) {
                    hasNextIssuePage = issues.pageInfo.hasNextPage
                    issuesEndCursor = issues.pageInfo.endCursor
                }
            }

            releaseIdToIssueIdsMap[release.id] = issuesCollectedSoFar
        }
    }

    fun getSprints(workspaceId: String): List<GetSprintsQuery.Node> = runBlocking {
        val sprints = ArrayList<GetSprintsQuery.Node>()
        var queryResult: GetSprintsQuery.Sprints?
        var hasNextPage = false
        var endCursor: String? = null

        do {
            val query = GetSprintsQuery(workspaceId, Optional.present(endCursor))
            queryResult = apolloClient.query(query).toFlow().single().data?.workspace?.sprints

            if (queryResult != null) {
                hasNextPage = queryResult.pageInfo.hasNextPage
                endCursor = queryResult.pageInfo.endCursor
                sprints.addAll(queryResult.nodes)
            }
        } while (hasNextPage)

        sprints
    }

    /** Cannot move an issue to closed because closed is not a pipeline. */
    fun moveIssueToPipeline(issueId: String, pipeline: Pipeline): MoveIssueMutation.MoveIssue? =
        runBlocking {
            val input = MoveIssueInput(Optional.absent(), pipeline.id, issueId, Optional.present(0))
            val mutation = MoveIssueMutation(input, DEFAULT_WORKSPACE_ID)
            apolloClient.mutation(mutation).toFlow().single().data?.moveIssue
        }

    fun setEstimate(issueId: String, value: Double?): SetEstimateMutation.SetEstimate? =
        runBlocking {
            val input =
                SetEstimateInput(Optional.absent(), Optional.presentIfNotNull(value), issueId)
            val mutation = SetEstimateMutation(input)
            apolloClient.mutation(mutation).toFlow().single().data?.setEstimate
        }

    fun closeIssues(issueIds: List<String>): CloseIssuesMutation.CloseIssues? = runBlocking {
        val mutation = CloseIssuesMutation(issueIds)
        apolloClient.mutation(mutation).toFlow().single().data?.closeIssues
    }

    fun getRelease(releaseId: String): Release? {
        var queryResult: GetReleaseQuery.OnRelease?
        val releaseIssueIds = mutableSetOf<String>()
        var endCursor: String? = null
        var hasNextPage: Boolean

        do {
            queryResult = getRelease(releaseId, endCursor)
            val pageIssues = queryResult?.issues?.nodes?.map { issue -> issue.id } ?: emptyList()
            releaseIssueIds.addAll(pageIssues)

            hasNextPage = queryResult?.issues?.pageInfo?.hasNextPage ?: false
            endCursor = queryResult?.issues?.pageInfo?.endCursor
        } while (hasNextPage)

        return queryResult?.let {
            Release(
                queryResult.id,
                queryResult.title,
                queryResult.state,
                LocalDate.parse(queryResult.startOn.toString()),
                LocalDate.parse(queryResult.endOn.toString()),
                releaseIssueIds)
        }
    }

    private fun getRelease(releaseId: String, endCursor: String?): GetReleaseQuery.OnRelease? =
        runBlocking {
            val query = GetReleaseQuery(releaseId, Optional.presentIfNotNull(endCursor))
            apolloClient.query(query).toFlow().single().data?.node?.onRelease
        }

    fun getReleaseByName(name: String): Release? = runBlocking {
        val releases: ArrayList<GetMinimalReleasesQuery.Node> = ArrayList()
        var endCursor: String? = null
        var hasNextPage: Boolean

        do {
            val releasesQuery =
                GetMinimalReleasesQuery(githubRepositoryId, Optional.presentIfNotNull(endCursor))
            val releasesInPage =
                apolloClient
                    .query(releasesQuery)
                    .toFlow()
                    .single()
                    .data
                    ?.repositoriesByGhId
                    ?.get(0)
                    ?.releases

            if (releasesInPage?.nodes != null) {
                releases.addAll(releasesInPage.nodes)
            }

            hasNextPage = releasesInPage?.pageInfo?.hasNextPage ?: false
            endCursor = releasesInPage?.pageInfo?.endCursor
        } while (hasNextPage)

        for (release in releases) {
            if (release.title == name) {
                return@runBlocking getRelease(release.id)
            }
        }

        throw IllegalArgumentException("Release with name $name not found")
    }

    fun addIssuesToRelease(
        issueIds: Set<String>,
        releaseId: String
    ): AddIssuesToReleasesMutation.Release? = runBlocking {
        val input =
            AddIssuesToReleasesInput(Optional.absent(), issueIds.toList(), listOf(releaseId))
        val mutation = AddIssuesToReleasesMutation(input)
        apolloClient
            .mutation(mutation)
            .toFlow()
            .single()
            .data
            ?.addIssuesToReleases
            ?.releases
            ?.get(0)
    }

    fun removeIssuesFromRelease(
        issueIds: Set<String>,
        releaseId: String
    ): RemoveIssuesFromReleasesMutation.Release? = runBlocking {
        val input =
            RemoveIssuesFromReleasesInput(Optional.absent(), issueIds.toList(), listOf(releaseId))
        val mutation = RemoveIssuesFromReleasesMutation(input)
        apolloClient
            .mutation(mutation)
            .toFlow()
            .single()
            .data
            ?.removeIssuesFromReleases
            ?.releases
            ?.get(0)
    }

    fun getIssueEvents(githubRepoId: Int, issueNumber: Int): ArrayList<GetIssueEventsQuery.Node> {
        val results = ArrayList<GetIssueEventsQuery.Node>()
        var endCursor: String? = null
        var hasNextPage: Boolean

        do {
            val pageEvents = getIssueEvents(githubRepoId, issueNumber, endCursor)
            results.addAll(pageEvents?.nodes ?: emptyList())
            hasNextPage = pageEvents?.pageInfo?.hasNextPage ?: false
            endCursor = pageEvents?.pageInfo?.endCursor
        } while (hasNextPage)

        return results
    }

    private fun getIssueEvents(
        githubRepoId: Int,
        issueNumber: Int,
        endCursor: String?
    ): GetIssueEventsQuery.TimelineItems? = runBlocking {
        val query =
            GetIssueEventsQuery(githubRepoId, issueNumber, Optional.presentIfNotNull(endCursor))
        apolloClient.query(query).toFlow().single().data?.issueByInfo?.timelineItems
    }

    fun createRelease(
        githubRepoId: Int,
        title: String,
        startOn: LocalDate,
        endOn: LocalDate
    ): CreateReleaseMutation.CreateRelease? = runBlocking {
        val input =
            CreateReleaseInput(
                Optional.absent(),
                ReleaseCreateInput(
                    title,
                    Optional.absent(),
                    startOn.toString(),
                    endOn.toString(),
                    listOf(githubRepoId)))
        val mutation = CreateReleaseMutation(input)
        apolloClient.mutation(mutation).toFlow().single().data?.createRelease
    }

    fun getEpicsForRepository(githubRepoId: Int): Set<EpicData> {
        val results = mutableSetOf<EpicData>()
        var epicsEndCursor: String? = null
        var hasNextPageOfEpics: Boolean

        do {
            val queryResults = getEpicsForRepository(githubRepoId, epicsEndCursor, null)
            results.addAll(extractEpicData(githubRepoId, epicsEndCursor))

            hasNextPageOfEpics = queryResults?.pageInfo?.hasNextPage ?: false
            epicsEndCursor = queryResults?.pageInfo?.endCursor
        } while (hasNextPageOfEpics)

        return results
    }

    private fun extractEpicData(
        githubRepoId: Int,
        epicsEndCursor: String?,
    ): Set<EpicData> {
        val epicIdToChildIssueIds = mutableMapOf<String, MutableSet<String>>()
        var hasNextPageOfChildIssues = false
        var childIssuesEndCursor: String? = null
        var queryResults: GetEpicsForRepositoriesQuery.Epics?

        do {
            queryResults = getEpicsForRepository(githubRepoId, epicsEndCursor, childIssuesEndCursor)
            queryResults?.nodes?.forEach { epic ->
                val childIssuesCollectedSoFar =
                    epicIdToChildIssueIds.getOrDefault(epic.id, mutableSetOf())
                childIssuesCollectedSoFar.addAll(epic.childIssues.nodes.map { node -> node.id })
                epicIdToChildIssueIds[epic.id] = childIssuesCollectedSoFar

                hasNextPageOfChildIssues =
                    hasNextPageOfChildIssues || epic.childIssues.pageInfo.hasNextPage
                if (epic.childIssues.pageInfo.hasNextPage) {
                    childIssuesEndCursor = epic.childIssues.pageInfo.endCursor
                }
            }
        } while (hasNextPageOfChildIssues)

        return queryResults
            ?.nodes
            ?.map { epicNode ->
                EpicData(
                    epicNode.id,
                    epicNode.issue.id,
                    epicIdToChildIssueIds[epicNode.id]?.toSet() ?: emptySet())
            }
            ?.toSet() ?: emptySet()
    }

    private fun getEpicsForRepository(
        githubRepoId: Int,
        epicsEndCursor: String?,
        childIssuesEndCursor: String?
    ): GetEpicsForRepositoriesQuery.Epics? = runBlocking {
        val query =
            GetEpicsForRepositoriesQuery(
                zenhubWorkspaceId,
                Optional.present(listOf(githubRepoId)),
                Optional.presentIfNotNull(epicsEndCursor),
                Optional.presentIfNotNull(childIssuesEndCursor))
        apolloClient.query(query).toFlow().single().data?.workspace?.epics
    }

    fun getEpicsByIds(epicIds: List<String>): List<EpicData> = runBlocking {
        val epics = mutableListOf<EpicData>()
        val numPages = epicIds.size / DEFAULT_PAGE_SIZE

        for (i in 0..numPages) {
            val query =
                GetEpicsByIdsQuery(
                    epicIds
                        .stream()
                        .skip(i * DEFAULT_PAGE_SIZE.toLong())
                        .limit(DEFAULT_PAGE_SIZE.toLong())
                        .toList())

            val queryResult = apolloClient.query(query).toFlow().single()

            if (queryResult.hasErrors()) {
                handleQueryErrors(queryResult.errors!![0], epicIds)
            }

            val epicsInPage = queryResult.data?.nodes?.mapNotNull { it?.onEpic } ?: emptyList()

            epics.addAll(
                epicsInPage.map {
                    EpicData(
                        it.id,
                        it.issue.id,
                        it.childIssues.nodes.map { childIssue -> childIssue.id }.toSet())
                })
        }

        epics
    }

    fun getPipelines(): List<GetPipelinesQuery.Node> = runBlocking {
        val query = GetPipelinesQuery(zenhubWorkspaceId)
        apolloClient.query(query).toFlow().single().data?.workspace?.pipelinesConnection?.nodes
            ?: emptyList()
    }

    fun getIssuesByIds(ids: Set<String>): Set<GetIssuesQuery.Issue> = runBlocking {
        val issues = mutableSetOf<GetIssuesQuery.Issue>()
        val numPages = ids.size / DEFAULT_PAGE_SIZE
        val idsList = ids.toList()

        for (i in 0..numPages) {
            val query =
                GetIssuesQuery(
                    idsList
                        .stream()
                        .skip(i * DEFAULT_PAGE_SIZE.toLong())
                        .limit(DEFAULT_PAGE_SIZE.toLong())
                        .toList())

            val issuesInPage =
                apolloClient.query(query).toFlow().single().data?.issues?.toSet() ?: emptySet()
            issues.addAll(issuesInPage)
        }

        issues
    }

    override fun close() {
        apolloClient.closeQuietly()
    }

    private fun getSprintByState(
        sprintFilters: SprintFiltersInput,
        orderSprintsBy: SprintOrderInput
    ): List<GetSprintsByStateQuery.Node> {
        val sprints: ArrayList<GetSprintsByStateQuery.Node> = ArrayList()
        var queryResult: GetSprintsByStateQuery.Data?
        var hasNextPage: Boolean
        var endCursor: String? = null

        do {
            queryResult = getSprintByState(sprintFilters, orderSprintsBy, endCursor)

            if (queryResult?.workspace?.sprints?.nodes != null) {
                sprints.addAll(queryResult.workspace!!.sprints.nodes)
            }

            hasNextPage = queryResult?.workspace?.sprints?.pageInfo?.hasNextPage ?: false
            endCursor = queryResult?.workspace?.sprints?.pageInfo?.endCursor
        } while (hasNextPage)

        return sprints
    }

    private fun getSprintByState(
        sprintFilters: SprintFiltersInput,
        orderSprintsBy: SprintOrderInput,
        endCursor: String?
    ): GetSprintsByStateQuery.Data? = runBlocking {
        val query =
            GetSprintsByStateQuery(
                zenhubWorkspaceId,
                sprintFilters,
                100,
                orderSprintsBy,
                Optional.presentIfNotNull(endCursor))
        apolloClient.query(query).toFlow().single().data
    }

    private fun searchClosedIssues(
        filters: IssueSearchFiltersInput,
        after: String?
    ): SearchClosedIssuesQuery.SearchClosedIssues? = runBlocking {
        val query =
            SearchClosedIssuesQuery(
                zenhubWorkspaceId, filters, Optional.present(100), Optional.presentIfNotNull(after))
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

        val indexOfEarliestIssue =
            results.indexOfFirst { issue ->
                Instant.parse(issue.issueFragment.closedAt.toString()).isBefore(startDate)
            }

        var indexOfLatestIssue = -1
        if (Instant.parse(results[0].issueFragment.closedAt.toString()).isAfter(endDate)) {
            indexOfLatestIssue =
                results.indexOfLast { issue ->
                    Instant.parse(issue.issueFragment.closedAt.toString()).isAfter(endDate)
                }
        }

        return results.subList(indexOfLatestIssue + 1, indexOfEarliestIssue)
    }

    private fun handleQueryErrors(
        error: com.apollographql.apollo3.api.Error,
        epicIds: List<String>
    ) {
        when (error.message) {
            "Invalid global id: Make sure id is in Base64 format" -> {
                throw IllegalArgumentException(error.message)
            }

            "Resource not found" -> {
                val missingEpicIndex =
                    (error.path?.get(1) as? Int)
                        ?: throw IllegalArgumentException("Value cannot be converted to Int")

                throw IllegalArgumentException(
                    "Failed to retrieve epic with ID: ${epicIds[missingEpicIndex]}")
            }
        }
    }
}
