package zenhub

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZenHubClientTest {
    private val zenHubClient = ZenHubClient()

    @Test
    fun whenIssueByInfoThenCorrectIssueIsReturned() {
        val issue = zenHubClient.issueByInfo(DEFAULT_GITHUB_REPOSITORY_ID, DEFAULT_GIT_REPOSITORY_ID, 18004)
        assertEquals(18004, issue?.number)
    }

    @Test
    fun whenGetIssuesByPipelineThenAtLeastZeroIssues() {
        val issues = zenHubClient.getIssuesByPipeline(Pipeline.MERGE_READY)

        if (issues.isNotEmpty()) {
            assertTrue(issues[0].number > 0)
        } else {
            assertNotNull(issues)
        }
    }

    @Test
    fun whenGetReleasesForValidRepoThenAtLeastOneRelease() {
        val releases = zenHubClient.getReleases(DEFAULT_GITHUB_REPOSITORY_ID)
        assertTrue(releases.isNotEmpty())
    }

    @Test
    fun whenGetReleasesForInvalidRepoThenNoReleases() {
        val releases = zenHubClient.getReleases(12345678)
        assertTrue(releases.isEmpty())
    }
}
