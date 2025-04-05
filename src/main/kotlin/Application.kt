package miet.lambda

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8081, host = "::") {
        val dataProvider = PostgresDatabaseDataProvider()
        val lambdaExecutorProvider = SingleLambdaExecutorProvider(RemoteLambdaExecutor())
        module(dataProvider, lambdaExecutorProvider)
    }.start(wait = true)
}

fun Application.module(dataProvider: DataProvider, lambdaExecutorProvider: LambdaExecutorProvider) {
    configureRouting(dataProvider, lambdaExecutorProvider)
}
