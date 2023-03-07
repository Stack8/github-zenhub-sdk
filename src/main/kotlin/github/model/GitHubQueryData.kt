package github.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(val data: SearchResultData)

@Serializable
data class SearchResultData(val search: SearchResultItemConnection)

@Serializable
data class SearchResultItemConnection(
    val issueCount: Int,
    val edges: List<SearchResultItemIssue>,
    val pageInfo: PageInfo
)

@Serializable
data class SearchResultItemIssue(val node: Issue)

@Serializable
data class Issue(val number: Int, val title: String, val assignees: UserConnection, val repository: Repository)

@Serializable
data class Repository(val name: String)

@Serializable
data class UserConnection(val nodes: List<User>)

@Serializable
data class User(val login: String)

@Serializable
data class PageInfo(val endCursor: String, val hasNextPage: Boolean)
