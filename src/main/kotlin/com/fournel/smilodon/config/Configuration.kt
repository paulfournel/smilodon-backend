package com.fournel.smilodon.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import java.util.*


@Configuration
class Configuration {
    @Bean
    fun restTemplate(logRequestResponseFilter: LogbookClientHttpRequestInterceptor): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.interceptors = listOf(logRequestResponseFilter);
        return restTemplate
    }

    @Bean(name = ["stravaRestTemplate"])
    fun restTemplateStrava(logRequestResponseFilter: LogbookClientHttpRequestInterceptor, stravaObjectMapper: ObjectMapper): RestTemplate {
        val messageConverter = MappingJackson2HttpMessageConverter()
        messageConverter.objectMapper = stravaObjectMapper
        messageConverter.supportedMediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON)

        val restTemplate = RestTemplate(listOf(messageConverter))
        restTemplate.interceptors = listOf(logRequestResponseFilter);
        return restTemplate
    }
}