package miet.lambda

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.StringValues
import io.ktor.util.toMap
import miet.lambda.db.DataProvider

fun Application.configureRouting(dataProvider: DataProvider, lambdaExecutorProvider: LambdaExecutorProvider) {
    routing {
        route(Regex("(?<service>[^/]+)/(?<lambda>.+)")) {
            handle {
                val service = call.parameters["service"]
                if (service == null) {
                    call.respondText("Service is not specified")
                    call.response.status(HttpStatusCode.BadRequest)
                    return@handle
                }

                val lambda = call.parameters["lambda"]
                if (lambda == null) {
                    call.respondText("Lambda is not specified")
                    call.response.status(HttpStatusCode.BadRequest)
                    return@handle
                }

                val lambdaInfo = dataProvider.getLambdaInfo(service, lambda)
                if (lambdaInfo == null) {
                    call.respondText("Lambda not found")
                    call.response.status(HttpStatusCode.NotFound)
                    return@handle
                }
                if (lambdaInfo.currentUserBalance - LAMBDA_EXECUTION_COST < 0) {
                    call.respondText("Not enough balance")
                    call.response.status(HttpStatusCode.Forbidden)
                    return@handle
                }

                val lambdaExecutor = lambdaExecutorProvider.findExecutorForLambda(lambdaInfo.id)

                val lambdaExecutorRequest = LambdaExecutorRequest(
                    method = call.request.httpMethod.value,
                    url = call.request.uri,
                    queryParameters = stringValuesToMap(call.request.queryParameters),
                    headers = stringValuesToMap(call.request.headers),
                    body = call.receiveText(),
                )

                when (val result = lambdaExecutor.execute(lambdaInfo.id, lambdaExecutorRequest)) {
                    is LambdaExecutionResult.Success -> {
                        call.response.status(HttpStatusCode.fromValue(result.response.statusCode))
                        result.response.headers?.forEach { (key, value) ->
                            call.response.headers.append(key, value)
                        }
                        result.response.body?.let { call.respondText(it) }

                        dataProvider.updateBalance(
                            lambdaInfo.ownerId,
                            LAMBDA_EXECUTION_COST,
                        )
                    }

                    is LambdaExecutionResult.Failure -> {
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.respondText(result.message)
                    }
                }
            }
        }
    }
}

private fun stringValuesToMap(stringValues: StringValues) = stringValues.toMap().mapValues {
    it.value.joinToString(", ")
}
