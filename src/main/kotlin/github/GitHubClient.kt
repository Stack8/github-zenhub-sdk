package github

import shared.GraphQueryHttpClient

internal object GitHubClient : GraphQueryHttpClient("https://api.github.com/graphql", System.getenv("GITHUB_API_TOKEN"))
