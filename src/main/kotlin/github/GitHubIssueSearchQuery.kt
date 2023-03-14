package github

class GitHubIssueSearchQuery(private val org: String, private val type: IssueType) {
    private var issueStatus: IssueStatus? = null
    private var closedOn: String? = null
    private var createdOn: String? = null

    fun issueStatus(issueStatus: IssueStatus): GitHubIssueSearchQuery {
        this.issueStatus = issueStatus
        return this
    }

    fun closedOn(closedOn: String): GitHubIssueSearchQuery {
        this.closedOn = closedOn
        return this
    }

    fun createdOn(createdOn: String): GitHubIssueSearchQuery {
        this.createdOn = createdOn
        return this
    }

    fun toQuery(): String {
        var query = "org:$org is:${type.value}"
        if (issueStatus != null) {
            query += " is:${issueStatus!!.value}"
        }
        if (closedOn != null) {
            query += " closed:$closedOn"
        }
        if (createdOn != null) {
            query += " created:$createdOn"
        }
        return query
    }
}

enum class IssueType(val value: String) {
    ISSUE("issue"), PR("pr")
}

enum class IssueStatus(val value: String) {
    OPEN("open"), CLOSED("closed")
}
