package com.fournel.smilodon.strava

import com.fournel.smilodon.user.SecurityUser
import com.fournel.smilodon.user.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.net.URL
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class StravaController(
    val userService: UserService,
    val stravaActivityService: StravaActivityService
) {
    @GetMapping("/api/connect_other_account")
    fun connectOtherAccount(
        request: HttpServletRequest, response: HttpServletResponse
    ) {
        val url = URL(request.requestURL.toString())
        val authentication = SecurityContextHolder.getContext().authentication
        val user = (authentication.principal as SecurityUser).user
        response.sendRedirect(
            "https://www.strava.com/api/v3/oauth/authorize?response_type=code&response_type=code&client_id=7500&scope=activity:read,profile:read_all&redirect_uri=${url.protocol}://${url.authority}/api/strava/${user.id}/${
                userService.getOneTimeCode(user)
            }"
        )
    }

    @GetMapping("/api/strava/{user_id}/{one_time_code}")
    fun linkOtherAccount(
        request: HttpServletRequest,
        httpResponse: HttpServletResponse,
        @RequestParam("code") code: String,
        @PathVariable("user_id") userId: String,
        @PathVariable("one_time_code") oneTimeCode: String,
    ) {
        userService.saveStravaUser(code, oneTimeCode)
        httpResponse.sendRedirect("/settings")
    }

    @GetMapping("/api/strava/sync")
    fun syncLastActivities() {
        stravaActivityService.processUser(userService.getCurrentUser())
    }
}



