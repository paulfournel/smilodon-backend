package com.fournel.smilodon.maps

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class MapController(
    val mapService: MapService
) {

    @GetMapping("/open-api")
    fun testPng(): ResponseEntity<ByteArray> {
        val points = mapService.processGPXFile("/Users/paulfournel/Downloads/Berlin_Marathon_2017_public.gpx")
            .map { LonLat(it.first, it.second) }

        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(mapService.createMap(points))
    }

}



