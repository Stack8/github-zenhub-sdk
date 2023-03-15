@file:JvmName("ZenHubIssueSearcher")

package zenhub.issue

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.serialization.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import zenhub.ZenHubClient
import java.time.Instant
import java.util.concurrent.CompletableFuture

@OptIn(DelicateCoroutinesApi::class)
fun query(startTime: Instant, endTime: Instant): CompletableFuture<List<ZenHubIssueSearchResult>> =
    GlobalScope.future { searchZenHubIssues(startTime, endTime) }


private suspend fun searchZenHubIssues(startTime: Instant, endTime: Instant): List<ZenHubIssueSearchResult> {
    var earliestClosedDate: Instant
    var cursor: String? = null
    val allResults = emptyList<ZenHubIssue>()

    do {
        val page = getPage(cursor)
        allResults.plus(page.data.issues.nodes)
        earliestClosedDate = Instant.parse(page.data.issues.nodes.last().closedAt)
        cursor = page.data.issues.pageInfo.endCursor
    } while (earliestClosedDate.isAfter(startTime))

    val trimmedResults = trimResults(allResults, startTime, endTime)
    return parseQueryResults(trimmedResults)
}

private suspend fun getPage(cursor: String?): ZenHubIssueQueryResult {
    var startCursor = ""
    if (cursor != null) {
        startCursor = "    after: \"$cursor\"\n"
    }
    return sendQuery(
        "{\n" +
                "  issues: searchClosedIssues(\n" +
                "    workspaceId: \"59c54eb49d9e774e473597f1\"\n" +
                "    filters: { labels: {nin: [\"Invalid\", \"Duplicate\", \"Epic\"] notInAny: true}}\n" +
                "    first: 100\n" +
                startCursor +
                "  ) {\n" +
                "    nodes {\n" +
                "      labels {\n" +
                "        nodes{\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "      number\n" +
                "      title\n" +
                "      user {\n" +
                "        login\n" +
                "      }\n" +
                "      assignees {\n" +
                "        nodes {\n" +
                "          login\n" +
                "        }\n" +
                "      }\n" +
                "      closedAt\n" +
                "      pullRequest\n" +
                "    }\n" +
                "    pageInfo{\n" +
                "      endCursor\n" +
                "    }\n" +
                "  }\n" +
                "}"
    )
}

private suspend fun sendQuery(query: String): ZenHubIssueQueryResult {
    val queryResponse = ZenHubClient.query(query)
    try {
        return queryResponse.body()
    } catch (e: JsonConvertException) {
        println("error, could not convert Json Body for json ${queryResponse.bodyAsText()}")
        throw e
    }
}

private fun trimResults(results: List<ZenHubIssue>, startDate: Instant, endDate: Instant): List<ZenHubIssue> {
    // The results are returned in reverse chronological order.

    val indexOfEarliestIssue = results.indexOfFirst { issue -> Instant.parse(issue.closedAt).isBefore(startDate) }

    var indexOfLatestIssue = -1
    if (Instant.parse(results[0].closedAt).isAfter(endDate)) {
        indexOfLatestIssue = results.indexOfLast { issue -> Instant.parse(issue.closedAt).isAfter(endDate) }
    }

    return results.subList(indexOfLatestIssue + 1, indexOfEarliestIssue)
}

private fun parseQueryResults(queryResults: List<ZenHubIssue>): List<ZenHubIssueSearchResult> {
    return queryResults.map { item ->
        ZenHubIssueSearchResult(
            item.number,
            item.title,
            item.pullRequest,
            item.user.login,
            item.assignees.nodes.map { user -> user.login }
        )
    }
}
