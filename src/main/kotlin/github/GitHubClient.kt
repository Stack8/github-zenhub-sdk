package github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.github.graphql.sdk.GetBranchLogHistoryQuery
import com.ziro.engineering.github.graphql.sdk.GetFileFromBranchQuery
import com.ziro.engineering.github.graphql.sdk.GetIssuesByFilterQuery
import com.ziro.engineering.github.graphql.sdk.RepositoryQuery
import com.ziro.engineering.github.graphql.sdk.type.IssueFilters
import kotlin.math.min
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly

const val MAX_COMMITS_IN_PAGE = 100

private const val DEFAULT_GITHUB_REPOSITORY_NAME = "smacs"
private const val DEFAULT_GITHUB_REPOSITORY_OWNER = "stack8"
private const val GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"

class GitHubClient : AutoCloseable {

    private val apolloClient: ApolloClient =
        ApolloClient.Builder()
            .serverUrl(GITHUB_GRAPHQL_URL)
            .addHttpHeader("Authorization", "Bearer ${System.getenv("GITHUB_API_TOKEN")}")
            .build()

    fun getRepository(
        repoName: String = DEFAULT_GITHUB_REPOSITORY_NAME,
        repoOwner: String = DEFAULT_GITHUB_REPOSITORY_OWNER
    ): RepositoryQuery.Repository? = runBlocking {
        val query = RepositoryQuery(repoName, repoOwner)
        apolloClient.query(query).toFlow().single().data?.repository
    }

    fun getIssueByFilter(
        repoName: String = DEFAULT_GITHUB_REPOSITORY_NAME,
        repoOwner: String = DEFAULT_GITHUB_REPOSITORY_OWNER,
        filter: IssueFilters
    ): List<GetIssuesByFilterQuery.Node?> = runBlocking {
        val issues = mutableListOf<GetIssuesByFilterQuery.Node?>()
        var hasNextPage = true
        var cursor = Optional.absent<String>()
        while (hasNextPage) {
            val query = GetIssuesByFilterQuery(repoOwner, repoName, filter, cursor)
            val response = apolloClient.query(query).toFlow().single()

            response.data?.repository?.issues?.edges?.forEach { edge -> issues.add(edge?.node) }

            response.data?.repository?.issues?.pageInfo?.let {
                cursor = Optional.presentIfNotNull(it.endCursor)
                hasNextPage = it.hasNextPage
            }
        }

        issues
    }

    fun getFileFromBranch(
        repoOwner: String = DEFAULT_GITHUB_REPOSITORY_OWNER,
        repoName: String = DEFAULT_GITHUB_REPOSITORY_NAME,
        branch: String,
        filePath: String,
    ): String? = runBlocking {
        val query = GetFileFromBranchQuery(repoOwner, repoName, "$branch:$filePath")
        apolloClient.query(query).toFlow().single().data?.repository?.`object`?.onBlob?.text
    }

    fun getCommits(
        repoOwner: String = DEFAULT_GITHUB_REPOSITORY_OWNER,
        repoName: String = DEFAULT_GITHUB_REPOSITORY_NAME,
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

    override fun close() {
        apolloClient.closeQuietly()
    }
}
