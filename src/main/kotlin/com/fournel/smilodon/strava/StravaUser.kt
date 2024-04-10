package com.fournel.smilodon.strava

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fournel.smilodon.user.User
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToOne

@Entity
data class StravaUser(
    @Id val id: Int,
    val firstname: String,
    val lastname: String,
    val country: String,
    val profileMedium: String,
    @JsonIgnore val token: String,
    @OneToOne
    val user: User
)
