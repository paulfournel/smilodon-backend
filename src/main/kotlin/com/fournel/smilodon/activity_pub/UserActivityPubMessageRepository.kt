package com.fournel.smilodon.activity_pub

import com.fournel.smilodon.user.User
import org.springframework.data.repository.Repository

interface UserActivityPubMessageRepository : Repository<UserActivityPubMessage, String> {
    fun findByUser(user: User): List<UserActivityPubMessage>
    fun save(userActivityPubMessage: UserActivityPubMessage): UserActivityPubMessage
}