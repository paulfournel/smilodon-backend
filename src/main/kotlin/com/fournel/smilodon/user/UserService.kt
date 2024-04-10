package com.fournel.smilodon.user

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fournel.smilodon.activity_pub.FollowEvent
import com.fournel.smilodon.activity_pub.RSAService
import com.fournel.smilodon.strava.StravaClient
import com.fournel.smilodon.strava.StravaUser
import com.fournel.smilodon.strava.StravaUserRepository
import com.fournel.smilodon.user.ROLE.USER
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.crypto.bcrypt.BCrypt.gensalt
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*
import java.util.UUID.randomUUID
import javax.persistence.*
import javax.transaction.Transactional

const val CONTENT_TYPE = "application/activity+json"

@Service
class UserService(
    val userRepository: UserRepository,
    val rsaService: RSAService,
    val stravaClient: StravaClient,
    val stravaUserRepository: StravaUserRepository
) {

    fun getUserById(userId: String): User {
        return userRepository.findById(userId)!!
    }

    fun searchById(userId: String): List<User> {
        return userId.split(" ").map { userRepository.searchUsers(it.lowercase(Locale.getDefault())) }.flatten()
            .distinctBy { it.id }
    }

    fun createUser(userTemplate: UserTemplate): User {
        if(userRepository.findByEmail(userTemplate.email)!=null){
            throw EmailAlreadyUsed()
        }
        if(userRepository.findById(userTemplate.username)!=null){
            throw UsernameAlreadyUsed()
        }

        val passwordHash = BCrypt.hashpw(userTemplate.password, gensalt())
        val keys = rsaService.generateKeys()
        return userRepository.save(
            User(
                userTemplate.username,
                userTemplate.email,
                passwordHash,
                USER.name,
                userTemplate.username,
                userTemplate.username,
                "",
                "",
                null,
                keys.public,
                keys.private
            )
        )
    }

    fun getOneTimeCode(user: User): String? {
        return userRepository.save(user.copy(oneTimeCode = randomUUID().toString())).oneTimeCode
    }

    fun getStravaUser(user: User): StravaUser? {
        return stravaUserRepository.findByUser(user)
    }

    fun saveStravaUser(accessToken: String, oneTimeCode: String) {
        val user = getCurrentUser()

        if (user.oneTimeCode != null && user.oneTimeCode != oneTimeCode) {
            throw RuntimeException("Unauthorized request")
        }
        val token = stravaClient.getUserProfileFromCode(accessToken)
        val userProfile = stravaClient.getUserProfile(token.accessToken)

        stravaUserRepository.save(
            StravaUser(
                userProfile.id,
                userProfile.firstname,
                userProfile.lastname,
                userProfile.country ?: "",
                userProfile.profile_medium ?: "",
                token.refreshToken,
                user
            )
        )
    }

    fun getCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val userStored = (authentication.principal as SecurityUser).user
        return getUserById(userStored.id)
    }

    fun patchUser(userTemplate: UserPatchTemplate): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = (authentication.principal as SecurityUser).user
        return userRepository.save(
            user.copy(
                username = userTemplate.username, firstName = userTemplate.firstName, lastName = userTemplate.lastName
            )
        )
    }

    fun follow(userId: String, followEvent: FollowEvent) {
        val user = userRepository.findById(userId)!!
        val server = URI(followEvent.actor).host
        if (user.followers.none { it.username == followEvent.actor }) {
            userRepository.save(
                user.copy(
                    followers = user.followers + Follower(
                        randomUUID().toString(), followEvent.actor, server
                    )
                )
            )
        }
    }

    fun following(userId: String, actor: Actor) {
        val user = userRepository.findById(userId)!!
        val server = URI(actor.id).host
        if (user.followings.none { it.username == actor.id }) {
            userRepository.save(
                user.copy(
                    followings = user.followings + Following(
                        randomUUID().toString(), actor.id, server
                    )
                )
            )
        }
    }

    @Transactional
    fun revokeStrava() {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = (authentication.principal as SecurityUser).user
        stravaUserRepository.deleteByUser(user)
    }
}

class EmailAlreadyUsed : RuntimeException()
class UsernameAlreadyUsed : RuntimeException()


data class ActorTemplate(
    val id: String,
    @JsonProperty("@context") val context: Any,
    val type: String,
    val preferredUsername: String,
    val name: String,
    val summary: String,
    val icon: List<String>?,
    val inbox: URI,
    val outbox: URI,
    val publicKey: PublicKey
) {
    @JsonIgnore
    fun toUserLink(): ActivityPubUserLink {
        return ActivityPubUserLink("self", CONTENT_TYPE, id)
    }
}

@Entity
data class Actor(
    @Id val id: String,
    val type: String,
    val preferredUsername: String,
    val name: String,
    val summary: String,
    @ElementCollection val icon: List<String>?,
    val inbox: String,
    val outbox: String,
    @ManyToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY) val publicKey: PublicKey
)

@Entity
data class PublicKey(
    @Id val id: String, val owner: String, val publicKeyPem: String
)

data class RSAKey(val public: String, val private: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActivityPubUserLink(val rel: String, val type: String?, val href: String?)