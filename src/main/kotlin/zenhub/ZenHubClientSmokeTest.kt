package zenhub

import java.time.Duration
import java.time.Instant

fun main() {
    ZenHubClient().use { client ->
        val issue = client.issueByInfo(15675)
        if (issue != null) {
            println("id: ${issue.id}")
            println("body: ${issue.body}")
        }
        val fourteenDays = Duration.ofDays(28)
        val response = client.searchClosedIssuesBetween(
            Instant.now().minus(fourteenDays),
            Instant.now()
        )
        println("Results Size: ${response.size}")

        response.forEach { node -> println(node) }

        val currentSprint = client.getCurrentSprint()
        println(currentSprint)

        println(client.getPreviousSprint())
    }
}

