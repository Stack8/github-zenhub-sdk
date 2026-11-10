package github

import com.ziro.engineering.github.graphql.sdk.GetChecksQuery
import com.ziro.engineering.github.graphql.sdk.GetStatusesQuery
import com.ziro.engineering.github.graphql.sdk.RepositoryQuery
import com.ziro.engineering.github.graphql.sdk.fragment.PullRequestFragment
import com.ziro.engineering.github.graphql.sdk.type.PullRequestMergeMethod
import com.ziro.engineering.github.graphql.sdk.type.PullRequestUpdateState
import java.net.URI

const val MAX_COMMITS_IN_PAGE = 100
const val MAX_STATUSES_IN_PAGE = 100

class GitHubClient : AutoCloseable {
    private val authHeader = "Bearer ${System.getenv("GITHUB_API_TOKEN")}"
    private val graphQlClient = GitHubGraphQlClient(authHeader)
    private val restClient = GitHubRestClient(authHeader)

    fun getRepository(
        repoName: String = DEFAULT_REPOSITORY_NAME,
        repoOwner: String = DEFAULT_REPOSITORY_OWNER
    ): RepositoryQuery.Repository? = graphQlClient.getRepository(repoName, repoOwner)

    fun getFileFromBranch(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        branch: String,
        filePath: String,
    ): String? = graphQlClient.getFileFromBranch(repoOwner, repoName, branch, filePath)

    fun getCommits(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        branch: String,
        numCommits: Int
    ): List<String> = graphQlClient.getCommits(repoOwner, repoName, branch, numCommits)

    fun getStatuses(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        gitReference: String
    ): List<GetStatusesQuery.Context> = graphQlClient.getStatuses(repoOwner, repoName, gitReference)

    /**
     * Unlike [getStatuses], which only has the latest status per context, this returns every status
     * posted to the commit.
     */
    fun getStatusHistory(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        gitReference: String
    ): List<CommitStatus> = restClient.getStatusHistory(repoOwner, repoName, gitReference)

    fun getChecks(
        repoOwner: String = DEFAULT_REPOSITORY_OWNER,
        repoName: String = DEFAULT_REPOSITORY_NAME,
        gitReference: String
    ): List<GetChecksQuery.Node1> = graphQlClient.getChecks(repoOwner, repoName, gitReference)

    fun createPullRequest(
        repoId: String,
        baseBranch: String,
        currBranch: String,
        title: String,
        body: String?,
    ): PullRequestFragment =
        graphQlClient.createPullRequest(repoId, baseBranch, currBranch, title, body)

    fun getPullRequestById(id: String): PullRequestFragment? = graphQlClient.getPullRequestById(id)

    fun getPullRequestsByIds(ids: Set<String>): List<PullRequestFragment?>? =
        graphQlClient.getPullRequestsByIds(ids)

    fun getPullRequestByUrl(url: URI): PullRequestFragment? = graphQlClient.getPullRequestByUrl(url)

    fun updatePullRequest(
        id: String,
        baseBranch: String?,
        body: String?,
        state: PullRequestUpdateState?
    ): PullRequestFragment = graphQlClient.updatePullRequest(id, baseBranch, body, state)

    fun mergePullRequest(
        id: String,
        commitTitle: String?,
        expectedHeadOid: String?,
        mergeMethod: PullRequestMergeMethod
    ) = graphQlClient.mergePullRequest(id, commitTitle, expectedHeadOid, mergeMethod)

    override fun close() {
        graphQlClient.close()
        restClient.close()
    }
}
