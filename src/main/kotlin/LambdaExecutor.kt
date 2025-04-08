package miet.lambda

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.routing.RoutingCall
import io.ktor.util.StringValues
import io.ktor.util.toMap

interface LambdaExecutor {
    suspend fun execute(
        lambdaId: Long,
        call: RoutingCall,
    ): LambdaExecutionResult
}

sealed interface LambdaExecutionResult {
    data class Success(
        val response: LambdaExecutorResponse,
    ) : LambdaExecutionResult

    data object Failure : LambdaExecutionResult
}

class RetryingLambdaExecutor(
    private val executorProvider: LambdaExecutorProvider,
) {
    suspend fun execute(
        lambdaId: Long,
        call: RoutingCall,
    ): LambdaExecutionResult {
        val executor = executorProvider.findExecutorForLambda(lambdaId)
        while (true) {
            val result = executor.execute(lambdaId, call)
            if (result is LambdaExecutionResult.Success) {
                return result
            }
        }
    }
}

class RemoteLambdaExecutor(
    private val client: HttpClient = HttpClient(),
) : LambdaExecutor {
    override suspend fun execute(
        lambdaId: Long,
        call: RoutingCall,
    ) = try {
        val lambdaRequest = LambdaExecutorRequest(
            method = call.request.httpMethod.value,
            url = call.request.uri,
            query = stringValuesToMap(call.request.queryParameters),
            headers = stringValuesToMap(call.request.headers),
            body = call.receiveText(),
        )

        val response = client.post {
            url("http://localhost:8080/v1/execute/lambda/$lambdaId")
            header("Content-Type", "application/json")
            setBody(lambdaRequest)
        }

        if (response.status.value == 200) {
            LambdaExecutionResult.Success(response.body())
        } else {
            LambdaExecutionResult.Failure
        }
    } catch (e: Exception) {
        LambdaExecutionResult.Failure
    }

    private fun stringValuesToMap(stringValues: StringValues) = stringValues.toMap().mapValues {
        it.value.joinToString(", ")
    }
}

class StubLambdaExecutor : LambdaExecutor {
    override suspend fun execute(lambdaId: Long, call: RoutingCall) = LambdaExecutionResult.Success(
        LambdaExecutorResponse(
            statusCode = 200,
            headers = mapOf(),
            body = "Executed lambda with id $lambdaId",
        ),
    )
}
