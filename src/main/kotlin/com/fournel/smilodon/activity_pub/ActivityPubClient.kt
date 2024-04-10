package com.fournel.smilodon.activity_pub

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fournel.smilodon.user.ActorTemplate
import com.fournel.smilodon.user.CONTENT_TYPE
import com.fournel.smilodon.user.User
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.security.MessageDigest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale.getDefault


@Service
class ActivityPubClient(val rsaService: RSAService, val restTemplate: RestTemplate, val objectMapper: ObjectMapper) {

    fun post(url: URI, message: Any, actor: ActorTemplate, user: User): JsonNode? {
        return exchange(POST, url, message, actor, user)
    }

    fun get(url: URI, actor: ActorTemplate, user: User): JsonNode? {
        return exchange(GET, url, null, actor, user)
    }

    fun <T> exchange(method: HttpMethod, url: URI, message: T?, actor: ActorTemplate, user: User): JsonNode? {
        val date = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"))
            .format(ZonedDateTime.now(ZoneId.of("UTC")))

        val digest = message?.let {
            val noteHash = MessageDigest.getInstance("SHA-256").digest(
                objectMapper.writeValueAsString(message).toByteArray(
                )
            )
            val noteHashBase64 = Base64.getEncoder().encodeToString(noteHash)

            "SHA-256=$noteHashBase64"
        }


        val signature = rsaService.createSignature(
            method.name.lowercase(getDefault()),
            url,
            date,
            digest,
            CONTENT_TYPE,
            actor,
            user
        )

        val headers = HttpHeaders()
        headers.add("host", url.authority)
        headers.add("date", date)
        headers.add("digest", digest)
        headers.add("signature", signature)
        headers.add("content-type", CONTENT_TYPE)

        val httpEntity = message?.let { HttpEntity(message, headers) } ?: HttpEntity(headers)

        return restTemplate.exchange(
            url.toString(), method, httpEntity, JsonNode::class.java, null
        ).body
    }

    fun getActor(actorUri: String): ActorTemplate {
        val headers = HttpHeaders()
        headers.add("content-type", CONTENT_TYPE)
        return restTemplate.exchange(
            actorUri, GET, HttpEntity("", headers), ActorTemplate::class.java
        ).body!!
    }
}