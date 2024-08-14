package github

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GitHubClientTest {
    private val gitHubClient = GitHubClient()

    @Test
    fun whenGetFileFromBranchThenFileIsNotEmpty() {
        val result = gitHubClient.getFileFromBranch(branch = "develop", filePath = "version.txt")
        assert(!result.isNullOrEmpty())
    }

    @Test
    fun whenGetRecentCommitsThenThereAre100Commits() {
        val result = gitHubClient.getRecentCommits(branch = "develop")
        assertEquals(100, result.size)
    }

    @Test
    fun whenGetRecentCommitsOnBadBranchThenThereAreNoCommits() {
        val result = gitHubClient.getRecentCommits(branch = "bad-branch")
        assertEquals(0, result.size)
    }
}