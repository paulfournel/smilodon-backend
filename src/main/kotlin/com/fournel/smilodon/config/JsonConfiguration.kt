package com.fournel.smilodon.config

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JsonConfiguration {

    @Bean(name = ["objectMapper", "jacksonObjectMapper"])
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .registerModule(KotlinModule.Builder().build())
            .registerModule(ParameterNamesModule())
            .setSerializationInclusion(NON_NULL)
    }

    @Bean(name = ["stravaObjectMapper"])
    fun stravaObjectMapper(): ObjectMapper {
        return ObjectMapper()
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .setPropertyNamingStrategy(SNAKE_CASE)
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .registerModule(KotlinModule.Builder().build())
            .registerModule(ParameterNamesModule())
            .setSerializationInclusion(NON_NULL)
    }
}