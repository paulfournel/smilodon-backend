package com.fournel.smilodon.activities

import com.fournel.smilodon.activity_pub.RSAService
import com.fournel.smilodon.user.SecurityUser
import com.fournel.smilodon.user.User
import com.fournel.smilodon.user.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.net.URL
import java.time.ZonedDateTime
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.math.max
import kotlin.math.min

@RestController
class ActivityController(
    val activityService: ActivityService,
    val rsaService: RSAService
) {

    @PostMapping("/api/comments")
    fun addComment(
        @RequestBody template: CommentTemplate
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = (authentication.principal as SecurityUser).user
        activityService.addComment(template, user)
    }

    @GetMapping("/open-api/activities/{activity-id}")
    fun getActivity(@PathVariable("activity-id") activityId: Long,
                    @RequestHeader headers: Map<String, String>): ActivityDto {
        rsaService.verifySignature(headers, "get /open-api/activities/$activityId")
        val activity = activityService.fetchActivity(activityId)
        val listOf = activity.data.toList().sortedBy { it.time }

        val dataPoints = mutableListOf<ActivityDataDto>()
        var timeToDeduct = 0.0
        var distanceToDeduct = 0.0
        for (i in 1 until listOf.size) {
            val distance = (listOf[i].distance!! - listOf[i - 1].distance!!)
            val time = (listOf[i].time!! - listOf[i - 1].time!!)
            val pace = if (distance != 0.0) (time / 60) / (distance / 1000) else null

            val data = listOf[i]

            if(pace != null && pace < 20){
                dataPoints.add(ActivityDataDto(
                    index = data.id!!,
                    elevation = data.altitude,
                    distance = data.distance!! - distanceToDeduct,
                    lat = data.latlng?.lat,
                    lng = data.latlng?.lng,
                    time = data.time!! - timeToDeduct,
                    pace = pace
                ))
            }else{
                timeToDeduct -= time
                distanceToDeduct -= distance
            }
        }

        val movingAvgWindow = 10
        val paces = dataPoints.map { it.pace }
        val dataDtoList = dataPoints.mapIndexed { index, data ->
            var pace: Double? = null
            if (index > 0) {
                val paceSum = paces.subList(max(0, index - movingAvgWindow), index).sumOf { it ?: 0.0 }
                pace = paceSum / min(index, movingAvgWindow)
            }

            data.copy(pace = pace)

        }.filter { it.pace != null && it.pace < 20 }

        return ActivityDto(
            id = activity.id,
            name = activity.name,
            type = activity.type,
            startDate = activity.startDate,
            timezone = activity.timezone,
            description = activity.description,
            distance = activity.distance,
            movingTime = activity.movingTime,
            elapsedTime = activity.elapsedTime,
            totalElevationGain = activity.totalElevationGain,
            activityType = activity.activityType,
            data = dataDtoList,
            mapImage = activity.mapImage,
            user = activity.user
        )
    }

}

data class ActivityDto(
    val id: Long,
    val name: String,
    val type: String,
    val startDate: ZonedDateTime,
    val timezone: String,
    val description: String?,
    val distance: Double,
    val movingTime: Int,
    val elapsedTime: Int,
    val totalElevationGain: Double,
    val activityType: ActivityType,
    val data: List<ActivityDataDto>,
    val mapImage: ByteArray?,
    val user: User
)

data class ActivityDataDto(
    val index: Long,
    val elevation: Double?,
    val distance: Double?,
    val lat: Double?,
    val lng: Double?,
    val time: Double?,
    val pace: Double?
)


