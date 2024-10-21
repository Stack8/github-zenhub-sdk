package zenhub

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZenHubClientTest {
    private val zenHubClient = ZenHubClient()

    @Test
    fun whenGetIssuesByPipelineThenAtLeastZeroIssues() {
        val result = zenHubClient.getIssuesByPipeline(Pipeline.MERGE_READY)

        if (result.isNotEmpty()) {
            assertTrue(result[0].number > 0)
        } else {
            assertNotNull(result)
        }
    }

    @Test
    fun whenGetReleasesForValidRepoThenLeastOneRelease() {
        val result = zenHubClient.getReleases(DEFAULT_GITHUB_REPOSITORY_ID)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun whenGetReleasesForInvalidRepoThenNoReleases() {
        val result = zenHubClient.getReleases(12345678)
        assertTrue(result.isEmpty())
    }

    @Test
    fun whenGetIssuesThenAtLeastOneIssue() {
        val result = zenHubClient.getIssues()
        assertTrue(result.isNotEmpty())
    }
}