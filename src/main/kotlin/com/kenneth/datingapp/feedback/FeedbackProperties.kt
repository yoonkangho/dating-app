package com.kenneth.datingapp.feedback

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.feedback")
data class FeedbackProperties(
    val promptDelayHours: Long = 3
)
