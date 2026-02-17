package com.kenneth.datingapp.matching

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.matching")
data class MatchingProperties(
    val okExpirationDays: Long = 7,
    val maxActiveOks: Int = 3,
    val proposalDeadlineHours: Long = 48
)
