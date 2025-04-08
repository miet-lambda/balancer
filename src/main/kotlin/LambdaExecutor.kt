package miet.lambda

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url

interface LambdaExecutor {
    suspend fun execute(
        lambdaId: Long,
        request: LambdaExecutorRequest,
    ): LambdaExecutionResult
}

sealed interface LambdaExecutionResult {
    data class Success(
        val response: LambdaExecutorResponse,
    ) : LambdaExecutionResult

    data class Failure(val message: String) : LambdaExecutionResult
}

class RetryingLambdaExecutor(
    private val executorProvider: LambdaExecutorProvider,
    private val maxRetries: Int = 3,
) : LambdaExecutor {
    override suspend fun execute(
        lambdaId: Long,
        request: LambdaExecutorRequest,
    ): LambdaExecutionResult {
        val executor = executorProvider.findExecutorForLambda(lambdaId)
        var retries = 0
        while (true) {
            val result = executor.execute(lambdaId, request)
            if (result is LambdaExecutionResult.Success) {
                return result
            }

            retries++
            if (retries >= maxRetries) {
                return result
            }
        }
    }
}

class RemoteLambdaExecutor(
    private val client: HttpClient,
) : LambdaExecutor {
    override suspend fun execute(
        lambdaId: Long,
        request: LambdaExecutorRequest,
    ) = try {
        val response = client.post {
            url("v1/execute/lambda/$lambdaId")
            header("Content-Type", "application/json")
            header("Accept", "application/json")
            setBody(request)
        }

        if (response.status.value == 200) {
            LambdaExecutionResult.Success(response.body())
        } else {
            LambdaExecutionResult.Failure("Remote lambda executor returned status code ${response.status.value}")
        }
    } catch (e: Exception) {
        LambdaExecutionResult.Failure("Remote lambda execution failed with exception: ${e.message}")
    }
}
