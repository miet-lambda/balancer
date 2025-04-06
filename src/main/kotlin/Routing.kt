package miet.lambda

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(dataProvider: DataProvider, lambdaExecutorProvider: LambdaExecutorProvider) {
    routing {
        route(Regex("(?<service>[^/]+)/(?<lambda>.+)")) {
            handle {
                val service = call.parameters["service"]
                if (service == null) {
                    call.respondText("Service is not specified")
                    return@handle
                }

                val lambda = call.parameters["lambda"]
                if (lambda == null) {
                    call.respondText("Lambda is not specified")
                    return@handle
                }

                val lambdaInfo = dataProvider.getLambdaInfo(service, lambda)
                if (lambdaInfo == null) {
                    call.respondText("Lambda not found")
                    return@handle
                }

                val lambdaExecutor = lambdaExecutorProvider.findExecutorForLambda(lambdaInfo.id)
                when (val result = lambdaExecutor.execute(lambdaInfo.id, call)) {
                    is LambdaExecutionResult.Success -> {
                        call.response.status(HttpStatusCode.fromValue(result.response.statusCode))

                        val responseBody = result.response.body
                        if (responseBody != null) {
                            call.respondText(responseBody)
                            return@handle
                        }
                    }

                    is LambdaExecutionResult.Failure -> call.respondText("Lambda execution failed")
                }
            }
        }
    }
}
