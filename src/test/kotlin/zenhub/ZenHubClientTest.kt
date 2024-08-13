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
}