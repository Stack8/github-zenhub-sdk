package github

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.serialization.*

suspend fun searchGitHubIssues(query: GitHubIssueSearchQuery): List<GitHubIssue> {
    var pageInfo: PageInfo? = null
    var results = listOf<GitHubIssue>()

    do {
        val page = getPage(query, pageInfo).data.search
        pageInfo = page.pageInfo
        val parsedResults = parseResults(page.edges)
        results = results.plus(parsedResults)
    } while (pageInfo!!.hasNextPage)

    return results
}

private suspend fun getPage(query: GitHubIssueSearchQuery, pageInfo: PageInfo?): SearchResult {
    var searchTerms = "search(type: ISSUE, first: 100"

    searchTerms += ", query: \"${query.toQuery()}\""

    if (pageInfo != null && pageInfo.hasNextPage) {
        searchTerms += ", after: \"${pageInfo.endCursor}\""
    }

    searchTerms += ")"

    println("searching GitHub with search terms $searchTerms")

    return sendQuery(
        "query {" +
                "$searchTerms {" +
                "  issueCount" +
                "  edges {" +
                "    node {" +
                "      ... on Issue {" +
                "        number" +
                "        title" +
                "        assignees(first: 5) {" +
                "          nodes {" +
                "            login" +
                "          }" +
                "        }" +
                "        repository {" +
                "          name" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "  pageInfo {" +
                "    endCursor" +
                "    hasNextPage" +
                "  }" +
                "}}"
    )
}

private suspend fun sendQuery(query: String): SearchResult {
    val queryResponse = GitHubClient.query(query)
    try {
        return queryResponse.body()
    } catch (e: JsonConvertException) {
        println(queryResponse.bodyAsText())
        throw e
    }
}

private fun parseResults(issues: List<SearchResultItemIssue>): List<GitHubIssue> {
    return issues.map { (item) ->
        GitHubIssue(
            item.number,
            item.title,
            item.assignees.nodes.map { (user) -> user },
            item.repository.name
        )
    }
}
