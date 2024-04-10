package com.fournel.smilodon.activity_pub

import org.springframework.data.repository.Repository

interface InboxRepository : Repository<InboxRaw, String> {
    fun save(label: InboxRaw): InboxRaw
    fun findById(id: String): InboxRaw
    fun findAll(): List<InboxRaw>
    fun findAllByProcessedFalse(): List<InboxRaw>
}