package com.kenneth.datingapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DatingAppApplication

fun main(args: Array<String>) {
	runApplication<DatingAppApplication>(*args)
}
