package com.fournel.smilodon.strava

import com.fournel.smilodon.user.User
import org.springframework.data.repository.Repository

interface StravaUserRepository : Repository<StravaUser, String> {
    fun save(user: StravaUser): StravaUser
    fun findAll(): List<StravaUser>
    fun findByUser(user: User): StravaUser?
    fun deleteByUser(user: User)
}