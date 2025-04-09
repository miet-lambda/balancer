package miet.lambda

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LambdaExecutorRequest(
    @SerialName("method") val method: String,
    @SerialName("url") val url: String,
    @SerialName("query") val queryParameters: Map<String, String>? = null,
    @SerialName("headers") val headers: Map<String, String>? = null,
    @SerialName("body") val body: String? = null,
)

@Serializable
data class LambdaExecutorResponse(
    @SerialName("status") val statusCode: Int,
    @SerialName("headers") val headers: Map<String, String>? = null,
    @SerialName("body") val body: String? = null,
)
