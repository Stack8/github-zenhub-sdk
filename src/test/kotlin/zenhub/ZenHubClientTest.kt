package zenhub

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ZenHubClientTest {
    private val zenHubClient = ZenHubClient()

    @Test
    fun whenGetIssuesByPipelineThenAtLeastZeroIssues() {
        val result = zenHubClient.getIssuesByPipeline(Pipeline.MERGE_READY)

        if (!result.isNullOrEmpty()) {
            assertTrue(result[0].number > 0)
        } else {
            assertTrue(result.isNullOrEmpty())
        }
    }

    @Test
    fun whenGetActiveReleasesThenAtLeastOneRelease() {
        zenHubClient.getActiveReleases()?.let { assertTrue(it.isNotEmpty()) }
    }
}