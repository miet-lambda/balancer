package miet.lambda

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LambdaExecutorProvider {
    suspend fun findExecutorForLambda(lambdaId: Long): LambdaExecutor
}

class SingleLambdaExecutorProvider(
    private val lambdaExecutor: LambdaExecutor,
) : LambdaExecutorProvider {
    override suspend fun findExecutorForLambda(lambdaId: Long) = lambdaExecutor
}

class RoundRobinPoolLambdaExecutorProvider(
    private val lambdaExecutors: List<LambdaExecutor>,
) : LambdaExecutorProvider {
    private var currentIndex = 0
    private val mutex = Mutex()

    override suspend fun findExecutorForLambda(lambdaId: Long) = mutex.withLock {
        val executor = lambdaExecutors[currentIndex]
        currentIndex = (currentIndex + 1) % lambdaExecutors.size
        executor
    }
}
