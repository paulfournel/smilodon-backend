package com.fournel.smilodon.activity_pub

import com.fasterxml.jackson.databind.JsonNode
import com.fournel.smilodon.user.ActorTemplate
import com.fournel.smilodon.user.SecurityUser
import com.fournel.smilodon.user.UserService
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@RestController
class ActivityPubController(
    val activityPubService: ActivityPubService,
    val rsaService: RSAService,
    val restTemplate: RestTemplate,
    val activityPubClient: ActivityPubClient,
    val userService: UserService,
) {

    @GetMapping("/.well-known/webfinger")
    fun webfinger(@RequestParam("resource") resource: String): WebFingerResponse {
        return activityPubService.getUser(resource)
    }

    @GetMapping("/open-api/users/{user-id}/actor", produces = ["application/activity+json"])
    fun actor(@PathVariable("user-id") userId: String): ActorTemplate {
        return activityPubService.getActor(userId)
    }

    @PostMapping("/open-api/users/{user-id}/inbox", produces = ["application/activity+json"])
    @ResponseStatus(ACCEPTED)
    fun inbox(
        @PathVariable("user-id") userId: String,
        @RequestBody body: JsonNode,
        @RequestHeader headers: Map<String, String>
    ) {
        rsaService.verifySignature(headers, "post /open-api/users/$userId/inbox")
        activityPubService.accept(userId, body)
    }

    @GetMapping("/open-api/users/{user-id}/outbox", produces = ["application/activity+json"])
    fun outbox(
        @PathVariable("user-id") userId: String, @RequestParam(name = "page", defaultValue = "false") page: Boolean
    ): Any {
        return if (page) {
            activityPubService.getOutbox(userId)
        } else {
            activityPubService.getOutboxOverview(userId)
        }
    }

    @GetMapping("/api/messages/{message-id}")
    fun getMessage(@PathVariable("message-id") messageId: String): ActivityMessageResponse {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = userService.getUserById((authentication.principal as SecurityUser).user.id)
        val actor = activityPubService.getActor(user.id)
        val message = activityPubService.getMessage(messageId)

        return ActivityMessageResponse(message, activityPubClient.get(URI.create(message.url), actor, user)!!)
    }

    @GetMapping("/open-api/users")
    fun getWebfinger(@RequestParam query: String): List<ActorTemplate> {
        val params = query.split("@")
        var url: String

        if (params.size == 3) {
            val username = params[1]
            val domain = params[2]
            val resource = "acct:$username@$domain"
            url = "https://$domain/.well-known/webfinger"
            val builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("resource", resource)
            url = builder.build().toUriString()

            val first = restTemplate.getForObject(
                url, WebFingerResponse::class.java
            )!!.links.first { it.type == "application/activity+json" }


            return listOf(activityPubClient.getActor(first.href!!))
        } else {
            return activityPubService.searchActor(query)
        }
    }
}

data class ActivityMessageResponse(val message: ActivityPubMessage, val activity: JsonNode)


