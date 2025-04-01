package miet.lambda

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8081, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRouting()
}
