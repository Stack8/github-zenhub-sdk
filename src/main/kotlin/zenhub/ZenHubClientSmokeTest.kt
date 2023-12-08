package zenhub

import java.time.Instant

fun main() {
    ZenHubClient().use { client ->
        val issue = client.issueByInfo(15675)
        if (issue != null) {
            println("id: ${issue.id}")
            println("body: ${issue.body}")
        }
        val response = client.searchClosedIssuesBetween(Instant.parse("2023-12-01T00:00:00.000Z"), Instant.now())
        println("Results Size: ${response.size}")
    }
}

