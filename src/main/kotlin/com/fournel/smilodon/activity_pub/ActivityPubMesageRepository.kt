package com.fournel.smilodon.activity_pub

import com.fournel.smilodon.user.Actor
import org.springframework.data.repository.Repository

interface ActivityPubMesageRepository : Repository<ActivityPubMessage, String> {
    fun save(label: ActivityPubMessage): ActivityPubMessage
    fun findById(id: String): ActivityPubMessage?
    fun findAll(): List<ActivityPubMessage>
    fun findFirstByAttributedToAndIsSportOrderByStartDateDesc(actor: Actor, isSport: Boolean = true): ActivityPubMessage?
    fun findAllByAttributedToAndIsSportOrderByStartDateDesc(actor: Actor, isSport: Boolean = true): List<ActivityPubMessage>
}