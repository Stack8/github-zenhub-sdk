package zenhub

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ZenHubClientTest {
    private val zenHubClient = ZenHubClient()

    @Test
    fun whenIssueByInfoThenCorrectIssueIsReturned() {
        val issue =
            zenHubClient.issueByInfo(DEFAULT_GITHUB_REPOSITORY_ID, DEFAULT_GIT_REPOSITORY_ID, 18004)
        assertEquals(18004, issue?.issueFragment?.number)
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
