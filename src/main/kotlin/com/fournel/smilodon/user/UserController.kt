package com.fournel.smilodon.user

import com.fournel.smilodon.activities.ActivityService
import com.fournel.smilodon.activities.MultiActivityTypeStats
import com.fournel.smilodon.activity_pub.ActivityPubMessage
import com.fournel.smilodon.activity_pub.ActivityPubService
import com.fournel.smilodon.strava.StravaUser
import org.springframework.http.MediaType.IMAGE_PNG
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    val userService: UserService, val activityService: ActivityService, val activityPubService: ActivityPubService

) {

    @PostMapping("/open-api/users")
    fun create(@RequestBody userTemplate: UserTemplate): User {
        return userService.createUser(userTemplate)
    }

    @PatchMapping("/api/users/me")
    fun update(@RequestBody userTemplate: UserPatchTemplate) {
        userService.patchUser(userTemplate)
    }

    @DeleteMapping("/api/users/me/apps/strava")
    fun deleteStrava() {
        userService.revokeStrava()
    }

    @PostMapping("/api/users/follow")
    fun follow(@RequestBody followTemplate: FollowTemplate) {
        activityPubService.follow(followTemplate)
    }

    @GetMapping("/api/users/me")
    fun getSelf(): UserView {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = userService.getUserById((authentication.principal as SecurityUser).user.id)
        val lastMessage = activityPubService.getLastMessage(user)
        val statistics = activityService.statistics(user)
        val stravaUser = userService.getStravaUser(user)

        return UserView(
            user.id,
            user.email,
            user.username,
            user.firstName,
            user.lastName,
            user.summary,
            user.followings,
            user.followers,
            stravaUser,
            lastMessage,
            statistics
        )
    }

    @GetMapping("/api/users/me/activities")
    fun getActivities(): List<ActivityPubMessage> {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = (authentication.principal as SecurityUser).user

        return activityPubService.getActivitiesToDisplay(user)
    }

    @GetMapping("/api/img/{activity_id}.png")
    fun getActivityImage(@PathVariable("activity_id") activityId: Long): ResponseEntity<ByteArray> {
        return ResponseEntity.ok().contentType(IMAGE_PNG).body(activityService.fetchActivity(activityId).mapImage)
    }
}

data class FollowTemplate(val userUrl: String)

data class UserPatchTemplate(val firstName: String, val lastName: String, val username: String)

data class UserView(
    val id: String,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val summary: String,
    val followings: Set<Following>,
    val followers: Set<Follower>,
    val strava: StravaUser?,
    val lastActivity: ActivityPubMessage?,
    val statistics: MultiActivityTypeStats
)

