package zenhub

import java.time.Instant

object Constants {
    const val GITHUB_REPOSITORY_ID: Int = 15617306
    const val GIT_REPOSITORY_ID: String = "Z2lkOi8vcmFwdG9yL1JlcG9zaXRvcnkvMjEwNTg"
    const val ZENHUB_WORKSPACE_ID: String = "59c54eb49d9e774e473597f1"
}

fun main() {
    ZenHubClient(
        Constants.GITHUB_REPOSITORY_ID,
        Constants.GIT_REPOSITORY_ID,
        Constants.ZENHUB_WORKSPACE_ID
    )
        .use { client ->
            val issue = client.issueByInfo(15675)
            if (issue != null) {
                println("id: ${issue.id}")
                println("body: ${issue.body}")
            }
            val response = client.searchClosedIssuesBetween(
                Instant.parse("2023-12-01T00:00:00.000Z"),
                Instant.now()
            )
            println("Results Size: ${response.size}")
        }
}

