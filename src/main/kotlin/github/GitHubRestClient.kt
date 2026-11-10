package github

import adapters.readCommitStatuses
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

private const val GITHUB_REST_MEDIA_TYPE = "application/vnd.github+json"

internal class GitHubRestClient(private val authorizationHeader: String) : AutoCloseable {
    private val client = OkHttpClient()

    fun getStatusHistory(
        repoOwner: String,
        repoName: String,
        gitReference: String
    ): List<CommitStatus> {
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host(GITHUB_API_HOST)
                .addPathSegment("repos")
                .addPathSegment(repoOwner)
                .addPathSegment(repoName)
                .addPathSegment("commits")
                .addPathSegment(gitReference)
                .addPathSegment("statuses")
                .addQueryParameter("per_page", MAX_STATUSES_IN_PAGE.toString())
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header("Authorization", authorizationHeader)
                .header("Accept", GITHUB_REST_MEDIA_TYPE)
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    "Failed to get status history for $gitReference: HTTP ${response.code}")
            }

            return BufferedSourceJsonReader(response.body!!.source()).readCommitStatuses()
        }
    }

    override fun close() {
        client.connectionPool.evictAll()
    }
}
