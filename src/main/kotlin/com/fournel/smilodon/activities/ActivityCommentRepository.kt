package com.fournel.smilodon.activities

import org.springframework.data.repository.Repository

interface ActivityCommentRepository : Repository<ActivityComment, Long> {
    fun save(user: ActivityComment): ActivityComment
    fun findAll(): List<ActivityComment>
}