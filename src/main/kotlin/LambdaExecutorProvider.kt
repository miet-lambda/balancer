package miet.lambda

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import miet.lambda.db.DataProvider

interface LambdaExecutorProvider {
    suspend fun findExecutorForLambda(lambdaId: Long): LambdaExecutor
}

class SingleLambdaExecutorProvider(
    private val lambdaExecutor: LambdaExecutor,
) : LambdaExecutorProvider {
    override suspend fun findExecutorForLambda(lambdaId: Long) = lambdaExecutor
}

class RoundRobinPoolLambdaExecutorProvider(
    private val dataProvider: DataProvider,
) : LambdaExecutorProvider {
    private var lambdaExecutors = listOf<LambdaExecutor>()
    private var currentIndex = 0
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            while (true) {
                updateExecutors()
                delay(10000)
            }
        }
    }

    override suspend fun findExecutorForLambda(lambdaId: Long) = mutex.withLock {
        val executor = lambdaExecutors[currentIndex]
        currentIndex = (currentIndex + 1) % lambdaExecutors.size
        executor
    }

    private suspend fun updateExecutors() {
        val executors = dataProvider.getActualExecutorsList()
        if (executors.isEmpty()) {
            return
        }

        val newExecutors = executors.map { executorInfo ->
            RemoteLambdaExecutor(
                client = HttpClient {
                    defaultRequest {
                        url("http://${executorInfo.ip}:${executorInfo.port}/")
                    }
                    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                        json()
                    }
                },
            )
        }

        mutex.withLock {
            lambdaExecutors = newExecutors
        }
    }
}
