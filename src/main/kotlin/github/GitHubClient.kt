package github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.ziro.engineering.github.graphql.sdk.GetBranchLogHistoryQuery
import com.ziro.engineering.github.graphql.sdk.GetFileFromBranchQuery
import com.ziro.engineering.github.graphql.sdk.RepositoryQuery
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly
import kotlin.math.ceil

const val MAX_COMMITS_IN_PAGE = 100

private const val DEFAULT_GITHUB_REPOSITORY_NAME = "smacs"
private const val DEFAULT_GITHUB_REPOSITORY_OWNER = "stack8"
private const val GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"

class GitHubClient : AutoCloseable {

    private val apolloClient: ApolloClient =
        ApolloClient.Builder().serverUrl(GITHUB_GRAPHQL_URL)
            .addHttpHeader("Authorization", "Bearer ${System.getenv("GITHUB_API_TOKEN")}")
            .build()

    fun getRepository(
        repoName: String = DEFAULT_GITHUB_REPOSITORY_NAME,
        repoOwner: String = DEFAULT_GITHUB_REPOSITORY_OWNER
    ): RepositoryQuery.Repository? = runBlocking {
        val query = RepositoryQuery(repoName, repoOwner)
        apolloClient.query(query).toFlow().single().data?.repository
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
        var numPages = ceil(numCommits.toDouble() / MAX_COMMITS_IN_PAGE).toInt()
        var hasNextPage = true
        var cursor = Optional.absent<String>()
        var commitsLeft = numCommits

        while (numPages > 0 && hasNextPage) {
            var numCommitsInPage = MAX_COMMITS_IN_PAGE

            if (commitsLeft < MAX_COMMITS_IN_PAGE) {
                numCommitsInPage = commitsLeft
            }

            val query = GetBranchLogHistoryQuery(repoOwner, repoName, branch, numCommitsInPage, cursor)
            val response = apolloClient.query(query).toFlow().single()

            commits.addAll(response.data?.repository?.ref?.target?.onCommit?.history?.edges?.mapNotNull {
                it?.node?.message
            } ?: emptyList())

            response.data?.repository?.ref?.target?.onCommit?.history?.pageInfo?.let {
                cursor = Optional.presentIfNotNull(it.endCursor)
                hasNextPage = it.hasNextPage
            }

            commitsLeft -= numCommitsInPage
            numPages--
        }

        commits
    }

    override fun close() {
        apolloClient.closeQuietly()
    }
}
