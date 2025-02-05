package github

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class GitHubClientTest {
    private val gitHubClient = GitHubClient()

    @Test
    fun whenGetFileFromBranchThenFileIsNotEmpty() {
        val result = gitHubClient.getFileFromBranch(branch = "develop", filePath = "version.txt")
        assert(!result.isNullOrEmpty())
    }

    @Test
    fun whenGetCommitsWithLess1PageThenCorrectAmountIsReturned() {
        val result =
            gitHubClient.getCommits(branch = "develop", numCommits = MAX_COMMITS_IN_PAGE / 10)
        assertEquals(MAX_COMMITS_IN_PAGE / 10, result.size)
    }

    @Test
    fun whenGetCommitsWith1PageThenCorrectAmountIsReturned() {
        val result = gitHubClient.getCommits(branch = "develop", numCommits = MAX_COMMITS_IN_PAGE)
        assertEquals(MAX_COMMITS_IN_PAGE, result.size)
    }

    @Test
    fun whenGetCommitsWithMoreThan1PageThenCorrectAmountIsReturned() {
        val result =
            gitHubClient.getCommits(branch = "develop", numCommits = MAX_COMMITS_IN_PAGE * 2)
        assertEquals(MAX_COMMITS_IN_PAGE * 2, result.size)
    }

    @Test
    fun whenGetCommitsWith1PageAndLeftoverThenCorrectAmountIsReturned() {
        val result =
            gitHubClient.getCommits(branch = "develop", numCommits = MAX_COMMITS_IN_PAGE * 2 - 1)
        assertEquals(MAX_COMMITS_IN_PAGE * 2 - 1, result.size)
    }

    @Test
    fun whenGetCommitsOnBadBranchThenThereAreNoCommits() {
        val result =
            gitHubClient.getCommits(branch = "bad-branch", numCommits = MAX_COMMITS_IN_PAGE)
        assertEquals(0, result.size)
    }
}
