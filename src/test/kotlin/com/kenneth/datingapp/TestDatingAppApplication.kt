package com.kenneth.datingapp

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<DatingAppApplication>().with(TestcontainersConfiguration::class).run(*args)
}
