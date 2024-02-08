package github

import com.apollographql.apollo3.ApolloClient
import com.ziro.engineering.github.graphql.sdk.GetRepositoryQuery
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly

class GitHubClient : AutoCloseable {

    private val apolloClient: ApolloClient = ApolloClient.Builder().serverUrl("https://api.github.com/graphql")
        .addHttpHeader("Authorization", "Bearer ${System.getenv("GITHUB_API_TOKEN")}")
        .build()

    object Constants {
        const val GITHUB_REPOSITORY_ID: Int = 15617306
        const val GIT_REPOSITORY_ID: String = "Z2lkOi8vcmFwdG9yL1JlcG9zaXRvcnkvMjEwNTg"
    }

    fun getRepository(repoName: String, repoOwner: String): GetRepositoryQuery.Repository? = runBlocking {
        val query = GetRepositoryQuery(repoName, repoOwner)
        apolloClient.query(query).toFlow().single().data?.repository
    }

    override fun close() {
        apolloClient.closeQuietly()
    }
}