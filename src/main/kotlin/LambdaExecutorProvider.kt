package miet.lambda

interface LambdaExecutorProvider {
    fun findExecutorForLambda(lambdaId: Long): LambdaExecutor
}

class SingleLambdaExecutorProvider(
    private val lambdaExecutor: LambdaExecutor,
) : LambdaExecutorProvider {
    override fun findExecutorForLambda(lambdaId: Long) = lambdaExecutor
}
