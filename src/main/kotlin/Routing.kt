package miet.lambda

import io.ktor.server.application.Application
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.copyTo

class LambdaInfo(
    val id: Long,
    val name: String,
    val userCurrentBalance: Double,
)

fun getLambdaId(service: String, lambda: String): Long {
    return 1 // db call. Probably we should take a little more info except just lambda id
}

fun findExecutorForLambda(id: Long): LambdaExecutor {
    return StubLambdaExecutor() // USE A THREAD SAFE POOL OF THEM
}

fun Application.configureRouting() {
    routing {
        route("/{service}/{lambda}") {
            handle {
                val service = call.parameters["service"]
                if (service == null) {
                    call.respondOutputStream {
                        write("Service is not specified".toByteArray())
                    }
                    return@handle
                }

                val lambda = call.parameters["lambda"]
                if (lambda == null) {
                    call.respondOutputStream {
                        write("Lambda is not specified".toByteArray())
                    }
                    return@handle
                }

                val lambdaId = getLambdaId(service, lambda)
                val lambdaExecutor = findExecutorForLambda(lambdaId)
                val response = lambdaExecutor.execute(lambdaId, call)

                call.respondBytesWriter {
                    response.copyTo(this)
                }
            }
        }
    }
}
