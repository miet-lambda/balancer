package miet.lambda

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
                val response = lambdaExecutor.execute(lambdaInfo.id, call)

                val responseJson = Json.parseToJsonElement(response)
                val statusCode = responseJson.jsonObject["statusCode"]?.jsonPrimitive?.int
                if (statusCode != null) {
                    call.response.status(HttpStatusCode.fromValue(statusCode))
                }

                val responseBody = responseJson.jsonObject["body"]?.jsonPrimitive?.content
                if (responseBody != null) {
                    call.respondText(responseBody)
                    return@handle
                }
            }
        }
    }
}
