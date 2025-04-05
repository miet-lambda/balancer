package miet.lambda

import kotlinx.serialization.Serializable

@Serializable
data class LambdaExecutorRequest(
    val method: String,
    val url: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
)

@Serializable
data class LambdaExecutorResponse(
    val statusCode: Int,
    val body: String,
)
