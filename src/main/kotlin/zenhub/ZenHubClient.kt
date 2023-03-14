package zenhub

import shared.GraphQueryHttpClient

object ZenHubClient : GraphQueryHttpClient("https://api.zenhub.com/public/graphql", System.getenv("ZENHUB_GRAPHQL_TOKEN"))