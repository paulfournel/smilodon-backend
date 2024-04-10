package com.fournel.smilodon.activity_pub

import com.fasterxml.jackson.databind.node.TextNode
import com.fournel.smilodon.activities.Activity
import com.fournel.smilodon.activities.ActivityComment
import com.fournel.smilodon.user.ActorTemplate
import com.fournel.smilodon.user.PublicKey
import com.fournel.smilodon.activity_pub.ActivityPubType.*
import com.fournel.smilodon.user.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.*
import java.util.UUID.randomUUID

const val CONTEXT = "https://www.w3.org/ns/activitystreams"

@Service
class ActivityPubFactory(
    val restTemplate: RestTemplate,
    @Value("\${smilodon.server}") val server: String,
) {

    fun <T> activityPubEvent(actorId: String, type: ActivityPubType, `object`: T): ActivityPubEvent<T> {
        return ActivityPubEvent(
            CONTEXT, "$server/${randomUUID()}", type, actorId, `object`
        )
    }

    fun outboxOverview(actorId: String, totalItems: Int): ActivityPubOutboxOverview {
        return ActivityPubOutboxOverview(
            CONTEXT,
            "$server/open-api/users/$actorId/outbox",
            OrderedCollection,
            totalItems,
            "$server/open-api/users/$actorId/outbox?page=true",
            "$server/open-api/users/$actorId/outbox?min_id=0&page=true"
        )
    }

    fun outbox(actorId: String, messages: List<ActivityPubEvent<ActivityPubMessageTemplate>>): ActivityPubOutbox {
        return ActivityPubOutbox(
            CONTEXT,
            "$server/open-api/users/$actorId/outbox?page=true",
            OrderedCollectionPage,
            "$server/open-api/users/$actorId/outbox",
            "$server/open-api/users/$actorId/outbox?page=true",
            messages
        )
    }

    fun commentPubActivity(
        actorId: String, comment: ActivityComment
    ): ActivityPubMessageTemplate {
        return ActivityPubMessageTemplate(
            "$server/open-api/comments/${comment.id}",
            Note,
            comment.message,
            comment.inReplyTo,
            now().format(ISO_ZONED_DATE_TIME),
            "$server/open-api/comments/${comment.id}",
            actorId,
            false,
            "",
            false,
            listOf(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    fun activityPubActivity(actorId: String, activity: Activity): ActivityPubMessageTemplate {
        return ActivityPubMessageTemplate(
            "$server/open-api/activities/${activity.id}",
            Note,
            activity.name,
            "",
            now().format(ISO_ZONED_DATE_TIME),
            "$server/open-api/activities/${activity.id}",
            actorId,
            false,
            activity.description ?: "",
            true,
            listOf(
                Media(
                    UUID.randomUUID().toString(),
                    "Document",
                    "image/png",
                    "${server}/api/img/${activity.id}.png",
                    "null",
                    "U4Q]~DESys-G%fxxEJIQpGJ3w2WZx[nmSxFq",
                    515,
                    412
                )
            ),
            activity.type,
            activity.startDate,
            activity.timezone,
            activity.distance,
            activity.movingTime,
            activity.elapsedTime,
            activity.totalElevationGain
        )
    }

    fun actor(user: User): ActorTemplate {
        return ActorTemplate(
            "$server/open-api/users/${user.username}/actor",
            TextNode.valueOf(CONTEXT),
            "Person",
            user.username,
            "${user.firstName} ${user.lastName}",
            user.summary,
            listOf("$server/open-api/profile_picture?user=${user.id}"),
            URI.create("$server/open-api/users/${user.username}/inbox"),
            URI.create("$server/open-api/users/${user.username}/outbox"),
            PublicKey(
                "$server/open-api/users/${user.username}/actor#main-key",
                "$server/open-api/users/${user.username}/actor",
                user.publicKey
            )
        )
    }

}

enum class ActivityPubType {
    Accept, Follow, Create, Note, OrderedCollection, OrderedCollectionPage
}
