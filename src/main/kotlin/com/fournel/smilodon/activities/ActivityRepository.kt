package com.fournel.smilodon.activities

import com.fournel.smilodon.user.User
import org.springframework.data.repository.Repository
import java.time.ZonedDateTime

interface ActivityRepository : Repository<Activity, Long> {
    fun save(user: Activity): Activity
    fun findAll(): List<Activity>
    fun findAllByUser(user: User): List<Activity>
    fun findById(id: Long): Activity
    fun findAllByUserAndStartDateAfter(user: User, startDate: ZonedDateTime): List<Activity>
    fun countByUser(user: User): Long
}