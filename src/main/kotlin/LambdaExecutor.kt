package miet.lambda

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveStream
import io.ktor.server.request.uri
import io.ktor.server.routing.RoutingCall
import io.ktor.util.StringValues
import io.ktor.util.toMap
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface LambdaExecutor {
    suspend fun execute(
        lambdaId: Long,
        call: RoutingCall,
    ): ByteReadChannel
}

@Serializable
private data class LambdaRequest(
    val method: String,
    val url: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
)

class RemoteLambdaExecutor : LambdaExecutor {
    private val client = HttpClient()

    override suspend fun execute(
        lambdaId: Long,
        call: RoutingCall,
    ): ByteReadChannel {
        val lambdaRequest = LambdaRequest(
            method = call.request.httpMethod.value,
            url = call.request.uri,
            query = stringValuesToMap(call.request.queryParameters),
            headers = stringValuesToMap(call.request.headers),
            body = call.receiveStream().readBytes().decodeToString(),
        )
        val requestJson = Json.encodeToString(lambdaRequest)

        val response = client.post {
            url("http://localhost:8080/v1/execute/lambda/$lambdaId")
            headers {
                append("Content-Type", "application/json")
            }
            setBody(requestJson)
        }
        return response.bodyAsChannel()
    }

    private fun stringValuesToMap(stringValues: StringValues) = stringValues.toMap().mapValues {
        it.value.joinToString(", ")
    }
}

class StubLambdaExecutor : LambdaExecutor {
    override suspend fun execute(lambdaId: Long, call: RoutingCall): ByteReadChannel {
        println("Executing lambda with id $lambdaId")
        return "Stub lambda result".byteInputStream().toByteReadChannel()
    }
}
