package com.fournel.smilodon.activity_pub


import javax.persistence.*

@Entity
data class InboxRaw(
    @Id var id: String,

    @Column var userId: String,

    @Column var message: String,

    @Column var processed: Boolean,
)