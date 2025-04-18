package miet.lambda

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import miet.lambda.db.PostgresDatabaseDataProvider

fun main() {
    embeddedServer(
        Netty,
        configure = {
            connector {
                host = "::"
                port = 8081
            }
            connectionGroupSize = 4
            workerGroupSize = 100
            callGroupSize = 100
        },
        module = {
            install(CallLogging)
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }

            val dataProvider = PostgresDatabaseDataProvider()

            val roundRobinPoolLambdaExecutorProvider = RoundRobinPoolLambdaExecutorProvider(dataProvider)

            val lambdaExecutorProvider = SingleLambdaExecutorProvider(
                lambdaExecutor = RetryingLambdaExecutor(roundRobinPoolLambdaExecutorProvider),
            )

            configureRouting(dataProvider, lambdaExecutorProvider)
        },
    ).start(wait = true)
}
