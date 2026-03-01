package com.kenneth.datingapp

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: List<FieldError>? = null
)

data class FieldError(
    val field: String,
    val message: String
)
