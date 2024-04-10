package com.fournel.smilodon.strava


import com.fournel.smilodon.activities.*
import com.fournel.smilodon.maps.LonLat
import com.fournel.smilodon.maps.MapService
import com.fournel.smilodon.user.User
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.persistence.*
import javax.transaction.Transactional

const val PAGE_SIZE = 200
const val PAGE_SIZE_SMALL = 20
const val INITIAL_PAGE = 1

@Service
class StravaActivityService(
    private val client: StravaClient,
    private val stravaUserRepository: StravaUserRepository,
    private val activityService: ActivityService
) {

    private val log = LoggerFactory.getLogger(StravaActivityService::class.java)

    fun processUser(user: User) {
        processRecentActivities(stravaUserRepository.findByUser(user)!!)
    }

    @Scheduled(fixedRate=60*60*1000)
    fun processAllUsers() {
        log.info("processing all users")
        stravaUserRepository.findAll().forEach { processRecentActivities(it) }
    }

    private fun getAllActivities(tokenPrivateUser: String): List<StravaActivityRaw> {

        val activities = mutableListOf<StravaActivityRaw>()
        var page = INITIAL_PAGE
        var activitiesPerPage = this.client.getActivities(tokenPrivateUser, PAGE_SIZE, page)
        while (activitiesPerPage.isNotEmpty()) {
            page++
            activities.addAll(activitiesPerPage)
            activitiesPerPage = this.client.getActivities(tokenPrivateUser, PAGE_SIZE, page)
        }
        return activities
    }

    private fun processAllActivities(stravaUser: StravaUser) {
        log.info("processing all activities for user ${stravaUser.id}")
        val tokenPrivateUser = this.client.refreshToken(stravaUser.token)!!.accessToken
        val allActivities = getAllActivities(tokenPrivateUser)
        importListOfActivities(allActivities, tokenPrivateUser, stravaUser)
    }

    private fun processRecentActivities(stravaUser: StravaUser) {
        log.info("processing user ${stravaUser.id}")
        val tokenPrivateUser = this.client.refreshToken(stravaUser.token)!!.accessToken
        val allActivities = this.client.getActivities(tokenPrivateUser, PAGE_SIZE_SMALL, INITIAL_PAGE)
        importListOfActivities(allActivities, tokenPrivateUser, stravaUser)
    }

    private fun importListOfActivities(
        allActivities: List<StravaActivityRaw>, tokenPrivateUser: String, domainUser: StravaUser
    ) {
        log.info("processing ${allActivities.size} activities for user ${domainUser.id}")

        allActivities.map { activity ->
            val streams = client.getActivityStreams(tokenPrivateUser, activity.id)
            val latLngStream = streams.first { it.type == "latlng" }
            val distanceStream = streams.first { it.type == "distance" }
            val altitudeStream = streams.first { it.type == "altitude" }
            val timeStream = streams.first { it.type == "time" }
            val dataPoints = (0 until streams.first().originalSize).map {
                ActivityData(
                    GPSPoint(
                        latLngStream.data[it.toInt()].get(0).asDouble(), latLngStream.data[it.toInt()].get(1).asDouble()
                    ),
                    distanceStream.data[it.toInt()].asDouble(),
                    altitudeStream.data[it.toInt()].asDouble(),
                    timeStream.data[it.toInt()].asDouble()
                )
            }
            val stravaActivity = Activity(
                activity.id,
                activity.name,
                activity.type,
                activity.startDate,
                activity.timezone,
                activity.description,
                activity.distance,
                activity.movingTime,
                activity.elapsedTime,
                activity.totalElevationGain,
                ActivityType.fromValue(activity.type),
                emptySet(),
                user = domainUser.user
            )
            activityService.save(stravaActivity, dataPoints)
        }
    }


}
