package com.fournel.smilodon.activities


import com.fournel.smilodon.activity_pub.ActivityPubService
import com.fournel.smilodon.maps.LonLat
import com.fournel.smilodon.maps.MapService
import com.fournel.smilodon.user.User
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.ZonedDateTime.now
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.transaction.Transactional

@Service
class ActivityService(
    private val activityRepository: ActivityRepository,
    private val activityCommentRepository: ActivityCommentRepository,
    private val mapService: MapService,
    val activityPubService: ActivityPubService
) {
    @Transactional
    fun fetchLastActivities(user: User): List<Activity> {
        return activityRepository.findAllByUser(user)
    }

    fun fetchActivity(activityId: Long): Activity {
        return activityRepository.findById(activityId)
    }

    fun addComment(commentTemplate: CommentTemplate, user: User) {
        val comment = activityCommentRepository.save(
            ActivityComment(
                UUID.randomUUID().toString(),
                commentTemplate.message,
                user.username,
                now(),
                commentTemplate.parentMessageUrl
            )
        )

        activityPubService.createComment(user, comment)
    }

    fun save(stravaActivity: Activity, dataPoints: List<ActivityData>) {
        val savedActivity = activityRepository.save(stravaActivity)

        val actUpdated = activityRepository.save(savedActivity.copy(data = dataPoints.toSet()))

        val img = mapService.createMap(actUpdated.data.map { LonLat(it.latlng!!.lng, it.latlng!!.lat) })

        activityRepository.save(actUpdated.copy(mapImage = img))

        activityPubService.createActivity(stravaActivity)
    }

    @Transactional
    fun statistics(user: User): MultiActivityTypeStats {
        val lastMonday = now().with(TemporalAdjusters.previous(MONDAY))
        val lastActivities = activityRepository.findAllByUserAndStartDateAfter(user, lastMonday)
        val countByUser = activityRepository.countByUser(user)
        return MultiActivityTypeStats(
            lastActivities
            .groupBy { it.activityType }.mapValues { makeStatsForSportType(it.value) }, countByUser)
    }

    private fun makeStatsForSportType(activities: List<Activity>): Stats {
        val weeklyHistogram = activities.groupBy { it.startDate.dayOfWeek }.mapValues { it.value.sumOf { it.distance } }
        return Stats(activities.sumOf { it.distance },
            activities.sumOf { it.movingTime },
            activities.sumOf { it.totalElevationGain },
            weeklyHistogram
        )
    }


}

data class CommentTemplate(val message: String, val parentMessageUrl: String)

data class Stats(
    val distance: Double,
    val movingTime: Int,
    val totalElevationGain: Double,
    val weeklyHistogram: Map<DayOfWeek, Double>
)

data class MultiActivityTypeStats(val statistics: Map<ActivityType, Stats>, val numberOfActivities: Long)