package github

import com.apollographql.apollo3.ApolloClient
import com.ziro.engineering.github.graphql.sdk.RepositoryQuery
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly

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

    override fun close() {
        apolloClient.closeQuietly()
    }
}