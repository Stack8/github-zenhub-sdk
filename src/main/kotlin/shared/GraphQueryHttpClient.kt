package shared

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

abstract class GraphQueryHttpClient(private val url: String, private val authToken: String) {
    private val httpClient: HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun query(query: String): HttpResponse {
        return httpClient.post(url) {
            header("Authorization", "bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(GraphQuery(query))
        }
    }
}