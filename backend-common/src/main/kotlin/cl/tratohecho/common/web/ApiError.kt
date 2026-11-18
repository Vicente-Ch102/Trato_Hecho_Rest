package cl.tratohecho.common.web

import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val details: List<String> = emptyList()
)
