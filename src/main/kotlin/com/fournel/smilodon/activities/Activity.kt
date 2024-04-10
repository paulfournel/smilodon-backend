package com.fournel.smilodon.activities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fournel.smilodon.user.User
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
data class Activity(
    @Id var id: Long,
    val name: String,
    val type: String,
    val startDate: ZonedDateTime,
    val timezone: String,
    val description: String?,
    val distance: Double,
    val movingTime: Int,
    val elapsedTime: Int,
    val totalElevationGain: Double,
    @Enumerated(EnumType.STRING) val activityType: ActivityType,
    @OneToMany(
        cascade = [CascadeType.ALL], fetch = FetchType.EAGER
    ) @JoinColumn(name = "activity_id") val data: Set<ActivityData>,
    @Lob @Column(columnDefinition = "oid") @JsonIgnore val mapImage: ByteArray? = null,
    @ManyToOne @JsonIgnore val user: User,
)

enum class ActivityType {
    Run, Ride, Swim, BackcountrySki, Other;

    companion object {
        fun fromValue(value: String): ActivityType = values().firstOrNull { it.name == value } ?: Other
    }
}