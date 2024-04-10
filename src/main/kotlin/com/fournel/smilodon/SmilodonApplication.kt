package com.fournel.smilodon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@EnableAsync
class SmilodonApplication

fun main(args: Array<String>) {
    runApplication<SmilodonApplication>(*args)
}
