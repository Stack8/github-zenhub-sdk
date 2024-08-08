package zenhub

fun main() {
    ZenHubClient().use { client ->
        println(client.getIssuesByPipeline(Pipeline.MERGE_READY))
    }
}

