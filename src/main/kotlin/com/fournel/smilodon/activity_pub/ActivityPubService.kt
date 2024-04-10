package com.fournel.smilodon.activity_pub

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fournel.smilodon.activities.Activity
import com.fournel.smilodon.activities.ActivityComment
import com.fournel.smilodon.activity_pub.ActivityPubType.*
import com.fournel.smilodon.user.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.net.URI
import java.security.MessageDigest.getInstance
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID.nameUUIDFromBytes
import java.util.UUID.randomUUID
import javax.persistence.*


@Service
class ActivityPubService(
    val objectMapper: ObjectMapper,
    val userService: UserService,
    val activityPubClient: ActivityPubClient,
    val inboxRepository: InboxRepository,
    val activityPubFactory: ActivityPubFactory,
    val activityPubMesageRepository: ActivityPubMesageRepository,
    val userActivityPubMessageRepository: UserActivityPubMessageRepository,
    val actorRepository: ActorRepository
) {

    fun accept(userId: String, jsonNode: JsonNode) {
        val inboxRaw = inboxRepository.save(
            InboxRaw(
                randomUUID().toString(), userId, objectMapper.writeValueAsString(jsonNode), false
            )
        )
        processInbox(inboxRaw)
    }

    @Async
    fun processInbox(inboxRaw: InboxRaw) {
        val readTree = objectMapper.readTree(inboxRaw.message)
        when (readTree.get("type").asText()) {
            Follow.name -> acceptFollow(inboxRaw.userId, readTree)
            Create.name -> acceptCreate(inboxRaw.userId, readTree)
            Accept.name -> followAccepted(inboxRaw.userId, readTree)
            else -> return
        }
        inboxRepository.save(inboxRaw.copy(processed = true))
    }

    fun getUser(resource: String): WebFingerResponse {
        val regex = Regex("(?<=acct:)(.*?)(?=@)")
        val userId = regex.find(resource)!!.value
        val userById = activityPubFactory.actor(userService.getUserById(userId))
        return WebFingerResponse(resource, listOf(userById.toUserLink()))
    }

    fun findUser(userId: String): WebFingerResponse {
        val userById = activityPubFactory.actor(userService.getUserById(userId))
        return WebFingerResponse("resource", listOf(userById.toUserLink()))
    }

    fun getOutboxOverview(actorId: String): ActivityPubOutboxOverview {
        return activityPubFactory.outboxOverview(actorId, 1)
    }

    fun getOutbox(actorId: String): ActivityPubOutbox {
        userService.getUserById(actorId)
        val activityPubActivities = getRecentMessages(actorId)
        val activityPubEvents = activityPubActivities.map {
            activityPubFactory.activityPubEvent(
                actorId, Create, ActivityPubMessageTemplate(
                    it.id,
                    it.type,
                    it.summary,
                    it.inReplyTo?.id,
                    it.published.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    it.url,
                    it.attributedTo.id,
                    it.sensitive,
                    it.content,
                    it.isSport,
                    it.attachment,
                    it.sportType,
                    it.startDate,
                    it.timezone,
                    it.distance,
                    it.movingTime,
                    it.elapsedTime,
                    it.totalElevationGain
                )
            )
        }
        return activityPubFactory.outbox(actorId, activityPubEvents)
    }

    fun getLastMessage(user: User): ActivityPubMessage? {
        val actorFromUrl = getActorFromUrl(URI.create(getActor(user.id).id))
        return activityPubMesageRepository.findFirstByAttributedToAndIsSportOrderByStartDateDesc(actorFromUrl)
    }

    fun getRecentMessages(actorId: String): List<ActivityPubMessage> {
        val actorFromUrl = getActorFromUrl(URI.create(getActor(actorId).id))
        return activityPubMesageRepository.findAllByAttributedToAndIsSportOrderByStartDateDesc(actorFromUrl)
    }

    fun getActor(userId: String): ActorTemplate {
        return activityPubFactory.actor(userService.getUserById(userId))
    }

    fun searchActor(searchQuery: String): List<ActorTemplate> {
        return userService.searchById(searchQuery).map { activityPubFactory.actor(it) }
    }

    fun getActorFromUrl(url: URI): Actor {
        val actorTemplate = activityPubClient.getActor(url.toString())
        return actorRepository.save(
            Actor(
                actorTemplate.id,
                actorTemplate.type,
                actorTemplate.preferredUsername,
                actorTemplate.name,
                actorTemplate.summary,
                actorTemplate.icon,
                actorTemplate.inbox.toString(),
                actorTemplate.outbox.toString(),
                actorTemplate.publicKey
            )
        )
    }

    fun follow(followTemplate: FollowTemplate) {
        val user = userService.getCurrentUser()
        val actor = activityPubFactory.actor(user)

        val inboxUri = activityPubClient.getActor(followTemplate.userUrl).inbox

        activityPubClient.post(
            inboxUri, activityPubFactory.activityPubEvent(actor.id, Follow, followTemplate.userUrl), actor, user
        )
    }

    @Async
    fun acceptFollow(userId: String, body: JsonNode) {
        val followEvent = objectMapper.convertValue(body, FollowEvent::class.java)
        val userToFollow = userService.getUserById(userId)
        val actorToFollow = activityPubFactory.actor(userToFollow)

        val inboxUri = activityPubClient.getActor(followEvent.actor).inbox
        val acceptFollowRequest = activityPubFactory.activityPubEvent(actorToFollow.id, Accept, followEvent)

        activityPubClient.post(inboxUri, acceptFollowRequest, actorToFollow, userToFollow)
        userService.follow(userId, followEvent)
    }

    fun followAccepted(userId: String, body: JsonNode) {
        val actor = getActorFromUrl(URI.create(body.get("actor").asText()))
        val currentUser = userService.getUserById(userId)
        val currentActor = activityPubFactory.actor(currentUser)
        userService.following(userId, actor)
        activityPubClient
            .get(URI.create(actor.outbox + "?page=true"), currentActor, currentUser)!!
            .get("orderedItems")
            .forEach {
                acceptCreate(userId, it)
            }
    }

    @Async
    fun acceptCreate(userId: String, body: JsonNode) {
        val activity = objectMapper.convertValue(body.get("object"), ActivityPubMessageTemplate::class.java)
        val userToNotify = userService.getUserById(userId)
        val inReplyTo = activityPubMesageRepository.findById(activity.inReplyTo ?: "")
        val actor = getActorFromUrl(URI.create(activity.attributedTo))

        val save = activityPubMesageRepository.save(
            ActivityPubMessage(
                hashToUUID(activity.id),
                activity.type,
                activity.summary ?: "",
                inReplyTo,
                ZonedDateTime.parse(activity.published),
                activity.url,
                actor,
                activity.sensitive,
                activity.content ?: "",
                activity.isSport ?: false,
                activity.attachment,
                activity.sportType,
                activity.startDate,
                activity.timezone,
                activity.distance,
                activity.movingTime,
                activity.elapsedTime,
                activity.totalElevationGain
            )
        )

        userActivityPubMessageRepository.save(
            UserActivityPubMessage(
                userToNotify.id + activity.id, userToNotify, save, save.published, false
            )
        )

    }

    fun createActivity(activity: Activity) {

        val actor = activityPubFactory.actor(activity.user)
        val activityPubActivity = activityPubFactory.activityPubActivity(actor.id, activity)
        val event = activityPubFactory.activityPubEvent(actor.id, Create, activityPubActivity)

        activityPubClient.post(actor.inbox, event, actor, activity.user)

        activity.user.followers.forEach {
            val inboxUri = activityPubClient.getActor(it.username).inbox
            activityPubClient.post(inboxUri, event, actor, activity.user)
        }

    }

    fun createComment(user: User, comment: ActivityComment) {
        val activityPubMessage = activityPubMesageRepository.findById(comment.inReplyTo)
        if (activityPubMessage != null) {
            val actor = activityPubFactory.actor(user)
            val activityPubActivity = activityPubFactory.commentPubActivity(actor.id, comment)
            val event = activityPubFactory.activityPubEvent(actor.id, Create, activityPubActivity)

            val inboxUri = activityPubClient.getActor(activityPubMessage.attributedTo.id).inbox
            activityPubClient.post(inboxUri, event, actor, user)
        }
    }

    fun getActivitiesToDisplay(user: User): List<ActivityPubMessage> {
        val findByUser = userActivityPubMessageRepository.findByUser(user)

        return findByUser.sortedByDescending { it.date }.map { it.activityPubMessage }.filter { it.inReplyTo == null }
    }

    private fun hashToUUID(input: String): String {
        val bytes = getInstance("SHA-256").digest(input.toByteArray())
        return nameUUIDFromBytes(bytes).toString()
    }

    fun getMessage(messageId: String): ActivityPubMessage {
        return activityPubMesageRepository.findById(messageId)!!
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebFingerResponse(val subject: String, val links: List<ActivityPubUserLink>)

data class ActivityPubEvent<T>(
    @JsonProperty("@context") val context: String,
    val id: String,
    val type: ActivityPubType,
    val actor: String,
    @JsonProperty("object") val `object`: T
)

data class ActivityPubOutboxOverview(
    @JsonProperty("@context") val context: String,
    val id: String,
    val type: ActivityPubType,
    val totalItems: Int,
    val first: String,
    val last: String
)

data class ActivityPubOutbox(
    @JsonProperty("@context") val context: String,
    val id: String,
    val type: ActivityPubType,
    val prev: String,
    val partOf: String,
    val orderedItems: List<ActivityPubEvent<ActivityPubMessageTemplate>>
)

data class FollowEvent(
    val id: String, val type: String, val actor: String, val `object`: String
)

data class ActivityPubMessageTemplate(
    val id: String,
    val type: ActivityPubType,
    val summary: String?,
    val inReplyTo: String?,
    val published: String,
    val url: String,
    val attributedTo: String,
    val sensitive: Boolean,
    val content: String?,
    val isSport: Boolean?,
    val attachment: List<Media>,
    val sportType: String?,
    val startDate: ZonedDateTime?,
    val timezone: String?,
    val distance: Double?,
    val movingTime: Int?,
    val elapsedTime: Int?,
    val totalElevationGain: Double?,
)

@Entity
data class UserActivityPubMessage(
    @Id val id: String,
    @ManyToOne(fetch = FetchType.LAZY) val user: User,
    @ManyToOne(fetch = FetchType.LAZY) val activityPubMessage: ActivityPubMessage,
    val date: ZonedDateTime,
    val isNotified: Boolean
)

@Entity
data class ActivityPubMessage(
    @Id val id: String,
    @Enumerated(EnumType.STRING) val type: ActivityPubType,
    val summary: String,
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) val inReplyTo: ActivityPubMessage? = null,
    val published: ZonedDateTime,
    val url: String,
    @ManyToOne(fetch = FetchType.LAZY) val attributedTo: Actor,
    val sensitive: Boolean,
    val content: String,
    val isSport: Boolean? = false,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true) val attachment: List<Media> = emptyList(),
    val sportType: String?,
    val startDate: ZonedDateTime?,
    val timezone: String?,
    val distance: Double?,
    val movingTime: Int?,
    val elapsedTime: Int?,
    val totalElevationGain: Double?,
    @OneToMany(mappedBy = "inReplyTo", fetch = FetchType.LAZY) val replies: List<ActivityPubMessage> = emptyList(),
)

@Entity
data class Media(
    @Id val id: String,
    val type: String,
    val mediaType: String,
    val url: String,
    val name: String?,
    val blurhash: String,
    val width: Int,
    val height: Int
)