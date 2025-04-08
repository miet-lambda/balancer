import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import miet.lambda.LambdaExecutorProvider
import miet.lambda.LambdaExecutorRequest
import miet.lambda.LambdaExecutorResponse
import miet.lambda.RemoteLambdaExecutor
import miet.lambda.configureRouting
import miet.lambda.db.DataProvider
import miet.lambda.db.ExecutorInfo
import miet.lambda.db.LambdaInfo
import org.junit.Test
import kotlin.test.assertEquals

class ExampleTest {
    @Test
    fun testRoot() = testApplication {
        externalServices {
            hosts("http://localhost:8080") {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }

                routing {
                    post("/v1/execute/lambda/{id}") {
                        val id = call.parameters["id"]?.toLongOrNull()
                        if (id == null) {
                            call.respondText("Lambda ID is not specified")
                            return@post
                        }
                        call.respond(
                            LambdaExecutorResponse(
                                200,
                                mapOf(),
                                "Executed lambda result",
                            ),
                        )
                    }
                }
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        application {
            val dataProvider = object : DataProvider {
                override suspend fun getLambdaInfo(service: String, lambda: String) = LambdaInfo(1, 1000.0)
                override suspend fun getActualExecutorsList() = listOf<ExecutorInfo>()
            }

            val lambdaExecutorProvider = object : LambdaExecutorProvider {
                override suspend fun findExecutorForLambda(lambdaId: Long) = RemoteLambdaExecutor(client)
            }

            configureRouting(dataProvider, lambdaExecutorProvider)
        }

        val response = client.post("http://localhost:80/service/lambda") {
            header("Content-Type", "application/json")
            setBody(
                LambdaExecutorRequest(
                    method = "POST",
                    url = "/service/lambda",
                    query = emptyMap(),
                    headers = emptyMap(),
                    body = "",
                ),
            )
        }

        assertEquals(200, response.status.value)
        assertEquals("Executed lambda result", response.bodyAsText())
    }
}
