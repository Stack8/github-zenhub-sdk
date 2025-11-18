package zenhub

import java.time.Duration
import java.time.Instant

fun main() {
    ZenHubClient().use { client ->
        val issue =
            client.issueByInfo(DEFAULT_GITHUB_REPOSITORY_ID, DEFAULT_ZENHUB_REPOSITORY_ID, 15675)
        if (issue != null) {
            println("id: ${issue.issueFragment.id}")
        }
        val fourteenDays = Duration.ofDays(28)
        val response =
            client.searchClosedIssues(Instant.now().minus(fourteenDays), Instant.now(), null)
        println("Results Size: ${response.size}")

        response.forEach { node -> println(node) }

        val currentSprint = client.getCurrentSprint()
        println(currentSprint)

        println(client.getPreviousSprint())
    }
}
