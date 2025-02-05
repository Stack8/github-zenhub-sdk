package github

fun main() {
    GitHubClient().use { client ->
        val repository = client.getRepository()
        if (repository != null) {
            println("repository: $repository")
            println(repository.milestones?.nodes?.get(0)?.title)
        }
    }
}
