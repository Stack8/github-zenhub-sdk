package zenhub

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ZenHubClientTest {
    private val zenHubClient = ZenHubClient()

    @Test
    fun whenGetActiveReleasesThenAtLeastOneRelease() {
        zenHubClient.getActiveReleases()?.let { assertTrue(it.isNotEmpty()) }
    }
}