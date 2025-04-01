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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel

interface LambdaExecutor {
    suspend fun execute(
        lambdaId: Long,
        request: RoutingCall,
    ): ByteReadChannel
}

class RemoteLambdaExecutor : LambdaExecutor {
    private val client = HttpClient()

    override suspend fun execute(
        lambdaId: Long,
        request: RoutingCall,
    ): ByteReadChannel {
        val requestJson = serializeRequestToJson(request)
        val response = client.post {
            url("http://localhost:8080/v1/execute/lambda/$lambdaId")
            headers {
                append("Content-Type", "application/json")
            }
            setBody(requestJson)
        }
        return response.bodyAsChannel()
    }

    private suspend fun serializeRequestToJson(call: RoutingCall): String {
        val request = call.request
        return """
            {
                "method": "${request.httpMethod.value}",
                "url": "${request.uri}",
                "query": "${request.queryParameters}",
                "headers": ${request.headers},
                "body": "${call.receiveStream().readBytes()}"
            }
        """.trimIndent()
    }
}

class StubLambdaExecutor : LambdaExecutor {
    override suspend fun execute(lambdaId: Long, request: RoutingCall): ByteReadChannel {
        println("Executing lambda with id $lambdaId")
        return "Stub lambda result".byteInputStream().toByteReadChannel()
    }
}
