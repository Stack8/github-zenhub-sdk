package zenhub

import shared.GraphQueryHttpClient

internal object LegacyZenHubClient : GraphQueryHttpClient("https://api.zenhub.com/public/graphql", System.getenv("ZENHUB_GRAPHQL_TOKEN"))
