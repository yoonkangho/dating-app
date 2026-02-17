package com.kenneth.datingapp

import com.kenneth.datingapp.feedback.FeedbackProperties
import com.kenneth.datingapp.matching.MatchingProperties
import com.kenneth.datingapp.matching.MeetingProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
	MatchingProperties::class,
	MeetingProperties::class,
	FeedbackProperties::class
)
class DatingAppApplication

fun main(args: Array<String>) {
	runApplication<DatingAppApplication>(*args)
}
