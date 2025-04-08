package miet.lambda

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import miet.lambda.db.PostgresDatabaseDataProvider

fun main() {
    embeddedServer(Netty, port = 8081, host = "::") {
        install(CallLogging)

        val dataProvider = PostgresDatabaseDataProvider()
        val lambdaExecutorProvider = SingleLambdaExecutorProvider(RemoteLambdaExecutor())
        configureRouting(dataProvider, lambdaExecutorProvider)
    }.start(wait = true)
}
