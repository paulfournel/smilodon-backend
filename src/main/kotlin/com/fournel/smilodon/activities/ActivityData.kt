package com.fournel.smilodon.activities

import javax.persistence.*

@Entity
@Table(name = "activity_data")
data class ActivityData(
    @Embedded var latlng: GPSPoint?, var distance: Double?, var altitude: Double?, var time: Double?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

@Embeddable
data class GPSPoint(
    @Column(name = "lat") var lat: Double,

    @Column(name = "lng") var lng: Double
)
