package miet.lambda

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LambdaExecutorRequest(
    val method: String,
    val url: String,
    @SerialName("query") val queryParameters: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
)

@Serializable
data class LambdaExecutorResponse(
    @SerialName("status") val statusCode: Int,
    val headers: Map<String, String>? = null,
    val body: String? = null,
)
