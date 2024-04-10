package com.fournel.smilodon.activities

import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "activity_comment")
data class ActivityComment(
    @Id var id: String,
    var message: String,
    var fromUser: String,
    val createdAt: ZonedDateTime,
    val inReplyTo: String,
)
