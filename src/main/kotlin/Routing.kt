package miet.lambda

import io.ktor.server.application.Application
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        post("balancer/") {
            call.respondOutputStream {
                call.receiveStream().copyTo(this)
            }
        }
    }
}
