package com.kenneth.datingapp.matching

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.meeting")
data class MeetingProperties(
    val maxWeeksFromMatch: Int = 3,
    val rescheduleLimit: Int = 1,
    val finalizationDeadlineHours: Long = 48
)
