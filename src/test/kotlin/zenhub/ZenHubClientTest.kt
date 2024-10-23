package zenhub

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZenHubClientTest {
    object TestConstants {
        const val VALID_RELEASE_ID = "Z2lkOi8vcmFwdG9yL1JlbGVhc2UvMTA0NTE1"
        const val VALID_RELEASE_NAME = "SMACS 10.0.0"
    }

    private val zenHubClient = ZenHubClient()

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
