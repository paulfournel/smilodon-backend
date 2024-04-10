package com.fournel.smilodon.strava

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fournel.smilodon.user.User
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.*
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.ZonedDateTime
import javax.persistence.*
import javax.persistence.InheritanceType.SINGLE_TABLE


@Service
class StravaClient(
    @Qualifier("stravaRestTemplate") private val restTemplate: RestTemplate,
    @Value("\${strava.client-id}") private val clientId: Int,
    @Value("\${strava.client-secret}") private val clientSecret: String,
    @Value("\${strava.base-url}") private val stravaBaseUrl: String
) {

    fun refreshToken(refreshToken: String): RefreshTokenResponse? {
        return restTemplate.exchange(
            "$stravaBaseUrl/oauth/token",
            POST,
            HttpEntity(RefreshTokenRequest(clientId, clientSecret, refreshToken), httpHeaders()),
            RefreshTokenResponse::class.java
        ).body
    }

    fun getActivities(accessToken: String, pageSize: Int, page: Int): List<StravaActivityRaw> {
        return restTemplate.exchange("$stravaBaseUrl/athlete/activities?per_page=$pageSize&page=$page",
            GET,
            HttpEntity<JsonNode>(httpHeaders(accessToken)),
            object : ParameterizedTypeReference<List<StravaActivityRaw>>() {}).body!!
    }

    fun getActivity(accessToken: String, activityId: Long): StravaActivityRaw {
        return restTemplate.exchange("$stravaBaseUrl/activities/$activityId?include_all_efforts=true",
            GET,
            HttpEntity<JsonNode>(httpHeaders(accessToken)),
            object : ParameterizedTypeReference<StravaActivityRaw>() {}).body!!
    }

    fun getActivityStreams(accessToken: String, activityId: Long): List<StravaActivityStream> {
        return restTemplate.exchange("$stravaBaseUrl/activities/${activityId}/streams?keys=distance,time,altitude,latlng",
            GET,
            HttpEntity<JsonNode>(httpHeaders(accessToken)),
            object : ParameterizedTypeReference<List<StravaActivityStream>>() {}).body!!
    }

    fun getUserProfile(accessToken: String): StravaUserProfile {
        return restTemplate.exchange("$stravaBaseUrl/athlete",
            GET,
            HttpEntity<JsonNode>(httpHeaders(accessToken)),
            object : ParameterizedTypeReference<StravaUserProfile>() {}).body!!
    }

    fun getUserProfileFromCode(code: String): RefreshTokenResponse {
        val request = StravaOauthTokenExchangeRequest(clientId, clientSecret, code, "authorization_code")
        return restTemplate.postForEntity(
            "$stravaBaseUrl/oauth/token",
            request,
            RefreshTokenResponse::class.java
        ).body!!
    }

    private fun httpHeaders(accessToken: String? = null, contentType: MediaType = APPLICATION_JSON): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = contentType
        if (accessToken != null) {
            headers["Authorization"] = "Bearer $accessToken"
        }
        return headers
    }

}

data class StravaUserProfile(
    val id: Int, val firstname: String, val lastname: String, val country: String?, val profile_medium: String?
)

data class RefreshTokenRequest(
    val clientId: Int, val clientSecret: String, val refreshToken: String, val grantType: String = "refresh_token"
)

data class RefreshTokenResponse(
    val tokenType: String, val accessToken: String, val expiresAt: Int, val expiresIn: Int, val refreshToken: String
)

data class StravaActivityRaw(
    val name: String,
    val id: Long,
    val type: String,
    val startDate: ZonedDateTime,
    val timezone: String,
    val description: String?,
    val distance: Double,
    val movingTime: Int,
    val elapsedTime: Int,
    val totalElevationGain: Double,
    val sportType: String
)


data class StravaActivityStream(
    val type: String, val seriesType: String, val originalSize: Long, val resolution: String, val data: List<JsonNode>
)

data class StravaUpload(val id: Long, val error: String?, val activityId: Long?)

data class StravaUpdatableActivity(val type: String? = null, val description: String? = null)

data class StravaOauthTokenExchangeRequest(
    val clientId: Int, val clientSecret: String, val code: String, val grantType: String
)