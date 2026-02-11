package github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.github.graphql.sdk.*
import com.ziro.engineering.github.graphql.sdk.fragment.PullRequestFragment
import com.ziro.engineering.github.graphql.sdk.type.CreatePullRequestInput
import com.ziro.engineering.github.graphql.sdk.type.MergePullRequestInput
import com.ziro.engineering.github.graphql.sdk.type.PullRequestMergeMethod
import com.ziro.engineering.github.graphql.sdk.type.PullRequestUpdateState
import com.ziro.engineering.github.graphql.sdk.type.UpdatePullRequestInput
import java.net.URI
import kotlin.math.min
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly

const val MAX_COMMITS_IN_PAGE = 100

private const val DEFAULT_REPOSITORY_NAME = "smacs"
private const val DEFAULT_REPOSITORY_OWNER = "stack8"
private const val GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"

class GitHubClient : AutoCloseable {

    private val apolloClient: ApolloClient =
        ApolloClient.Builder()
            .serverUrl(GITHUB_GRAPHQL_URL)
            .addHttpHeader("Authorization", "Bearer ${System.getenv("GITHUB_API_TOKEN")}")
            .build()

    fun getRepository(
        repoName: String = DEFAULT_REPOSITORY_NAME,
        repoOwner: String = DEFAULT_REPOSITORY_OWNER
    ): RepositoryQuery.Repository? = runBlocking {
        val query = RepositoryQuery(repoName, repoOwner)
        apolloClient.query(query).toFlow().single().data?.repository
    }

    fun getFileFromBranch(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        branch: String,
        filePath: String,
    ): String? = runBlocking {
        val query = GetFileFromBranchQuery(repoOwner, repoName, "$branch:$filePath")
        apolloClient.query(query).toFlow().single().data?.repository?.`object`?.onBlob?.text
    }

    fun getCommits(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        branch: String,
        numCommits: Int
    ): List<String> = runBlocking {
        val commits = mutableListOf<String>()
        var hasNextPage = true
        var cursor = Optional.absent<String>()
        var commitsLeft = numCommits

        while (commitsLeft > 0 && hasNextPage) {
            val numCommitsInPage = min(MAX_COMMITS_IN_PAGE, commitsLeft)
            val query =
                GetBranchLogHistoryQuery(repoOwner, repoName, branch, numCommitsInPage, cursor)
            val response = apolloClient.query(query).toFlow().single()

            commits.addAll(
                response.data?.repository?.ref?.target?.onCommit?.history?.edges?.mapNotNull {
                    it?.node?.message
                } ?: emptyList())

            response.data?.repository?.ref?.target?.onCommit?.history?.pageInfo?.let {
                cursor = Optional.presentIfNotNull(it.endCursor)
                hasNextPage = it.hasNextPage
            }

            commitsLeft -= numCommitsInPage
        }

        commits
    }

    fun getStatuses(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        gitReference: String
    ): List<GetStatusesQuery.Context> = runBlocking {
        val query = GetStatusesQuery(repoOwner, repoName, gitReference)
        apolloClient
            .query(query)
            .toFlow()
            .single()
            .data
            ?.repository
            ?.`object`
            ?.onCommit
            ?.status
            ?.contexts ?: emptyList()
    }

    fun getChecks(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        gitReference: String
    ): List<GetChecksQuery.Node1> = runBlocking {
        val query = GetChecksQuery(repoOwner, repoName, gitReference)

        apolloClient
            .query(query)
            .toFlow()
            .single()
            .data
            ?.repository
            ?.`object`
            ?.onCommit
            ?.checkSuites
            ?.nodes
            ?.first()
            ?.checkRuns
            ?.nodes
            ?.filterNotNull()
            .orEmpty()
    }

    fun createPullRequest(
        repoId: String,
        baseBranch: String,
        currBranch: String,
        title: String,
        body: String?,
    ): PullRequestFragment = runBlocking {
        val input =
            CreatePullRequestInput(
                clientMutationId = Optional.absent(),
                repositoryId = repoId,
                baseRefName = baseBranch,
                headRefName = currBranch,
                headRepositoryId = Optional.absent(),
                title = title,
                body = Optional.present(body),
                maintainerCanModify = Optional.absent(),
                draft = Optional.absent(),
            )

        val mutation = CreatePullRequestMutation(input)
        val response = apolloClient.mutation(mutation).execute()

        if (response.hasErrors()) {
            val exception = Exception(response.errors?.joinToString { it.message })
            throw IllegalStateException(exception)
        }

        val pullRequestFragment = response.data?.createPullRequest?.pullRequest?.pullRequestFragment

        if (pullRequestFragment == null) {
            val exception = Exception("Pull request fragment is null")
            throw IllegalStateException(exception)
        }

        pullRequestFragment
    }

    fun getPullRequestById(id: String) = runBlocking {
        val query = GetPullRequestsByIdsQuery(listOf(id))
        val nodes = apolloClient.query(query).toFlow().single().data?.nodes

        if (nodes.isNullOrEmpty()) {
            null
        } else {
            nodes[0]?.onPullRequest?.pullRequestFragment
        }
    }

    fun getPullRequestsByIds(ids: Set<String>) = runBlocking {
        val query = GetPullRequestsByIdsQuery(ids.toList())
        apolloClient.query(query).toFlow().single().data?.nodes?.map {
            it?.onPullRequest?.pullRequestFragment
        }
    }

    fun getPullRequestByUrl(url: URI) = runBlocking {
        val query = GetPullRequestByUrlQuery(url)
        apolloClient
            .query(query)
            .toFlow()
            .single()
            .data
            ?.resource
            ?.onPullRequest
            ?.pullRequestFragment
    }

    fun updatePullRequest(
        id: String,
        baseBranch: String?,
        body: String?,
        state: PullRequestUpdateState?
    ): PullRequestFragment = runBlocking {
        val input =
            UpdatePullRequestInput(
                pullRequestId = id,
                baseRefName = Optional.presentIfNotNull(baseBranch),
                body = Optional.presentIfNotNull(body),
                state = Optional.presentIfNotNull(state))

        val mutation = UpdatePullRequestMutation(input)
        val response = apolloClient.mutation(mutation).execute()

        if (response.hasErrors()) {
            val exception = Exception(response.errors?.joinToString { it.message })
            throw IllegalStateException(exception)
        }

        val pullRequestFragment = response.data?.updatePullRequest?.pullRequest?.pullRequestFragment

        if (pullRequestFragment == null) {
            val exception = Exception("Pull request fragment is null")
            throw IllegalStateException(exception)
        }

        pullRequestFragment
    }

    fun mergePullRequest(
        id: String,
        commitTitle: String?,
        expectedHeadOid: String?,
        mergeMethod: PullRequestMergeMethod
    ) = runBlocking {
        val input =
            MergePullRequestInput(
                commitHeadline = Optional.presentIfNotNull(commitTitle),
                expectedHeadOid = Optional.presentIfNotNull(expectedHeadOid),
                mergeMethod = Optional.present(mergeMethod),
                pullRequestId = id)

        val mutation = MergePullRequestMutation(input)
        val response = apolloClient.mutation(mutation).execute()

        if (response.hasErrors()) {
            throw RuntimeException(response.errors?.joinToString(separator = "\n") { it.message })
        }
    }

    override fun close() {
        apolloClient.closeQuietly()
    }
}
