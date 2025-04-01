package miet.lambda

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.uri

fun main() {
    embeddedServer(Netty, port = 8080, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val plugin =
        createApplicationPlugin(name = "RequestLoggingPlugin") {
            onCall { call ->
                println(call.request.uri)
            }
        }

    install(plugin)

    configureRouting()
}
