package com.fournel.smilodon.maps

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import kotlin.math.*

@Service
class MapService(val restTemplate: RestTemplate) {
    fun createMap(points: List<LonLat>): ByteArray {

        val minLat = points.minBy { it.lat }.lat
        val maxLat = points.maxBy { it.lat }.lat
        val minLon = points.minBy { it.lon }.lon
        val maxLon = points.maxBy { it.lon }.lon


        val zoomLevel = min(14, ceil(log2(360.0 / (maxLat - minLat))).toInt())

        val topLeftTile = getTileCoords(minLon, minLat, zoomLevel)
        val bottomRightTile = getTileCoords(maxLon, maxLat, zoomLevel)

        val startX = min(topLeftTile.xTile, bottomRightTile.xTile) - 1
        val startY = min(topLeftTile.yTile, bottomRightTile.yTile) - 1
        val endX = max(topLeftTile.xTile, bottomRightTile.xTile) + 1
        val endY = max(topLeftTile.yTile, bottomRightTile.yTile) + 1


        val numTilesX = endX - startX + 1
        val numTilesY = endY - startY + 1

        val tileSize = 256
        val imageWidth = tileSize * numTilesX
        val imageHeight = tileSize * numTilesY
        val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)

        // Fetch tiles and draw GPS trace
        var i = 0
        val g = image.createGraphics()
        for (y in startY..endY) {
            for (x in startX..endX) {
                val tileImage = fetchTileImage(zoomLevel, x, y)
                val tileX = (x - startX) * tileSize
                val tileY = (y - startY) * tileSize
                g.drawImage(tileImage, tileX, tileY, null)
                i++
            }
        }
        // Draw GPS points
        g.color = Color.RED
        g.stroke = BasicStroke(2.0f)


        g.color = Color.BLUE

        for (point in points) {
            val xTile = ((point.lon + 180) / 360 * (1 shl zoomLevel)).toInt()
            val yTile = ((1 - ln(tan(Math.toRadians(point.lat)) + 1 / cos(Math.toRadians(point.lat))) / PI) / 2 * (1 shl zoomLevel)).toInt()

            val pixelX = (xTile - startX)*tileSize + ((point.lon + 180) / 360 * tileSize * (1 shl zoomLevel) % tileSize).toInt()
            val pixelY = (yTile - startY)*tileSize + ((1 - ln(tan(Math.toRadians(point.lat)) + 1 / cos(Math.toRadians(point.lat))) / PI) / 2 * tileSize * (1 shl zoomLevel) % tileSize).toInt()
            g.fillOval(pixelX, pixelY, 6, 6)
        }
        g.dispose()

        // Write image to byte array
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    fun fetchTileImage(zoomLevel: Int, x: Int, y: Int): BufferedImage {
        val url = "https://tile.openstreetmap.org/$zoomLevel/$x/$y.png"
        //val url = "https://a.tile.opentopomap.org/${zoomLevel}/${x}/${y}.png"
        val headers = HttpHeaders()
        headers["authority"] = "tile.openstreetmap.org"
        headers["user-agent"] = "paul"

        val entity = HttpEntity<String>("", headers)

        val response = restTemplate.exchange(url, HttpMethod.GET, entity, ByteArray::class.java)
        return ImageIO.read(response.body!!.inputStream())
    }

    fun getTileCoords(lon: Double, lat: Double, zoom: Int): Tile {
        val xTile = ((lon + 180) / 360 * (1 shl zoom)).toInt()
        val yTile = ((1 - ln(tan(Math.toRadians(lat)) + 1 / cos(Math.toRadians(lat))) / PI) / 2 * (1 shl zoom)).toInt()
        return Tile(xTile, yTile, zoom)
    }

    fun processGPXFile(filePath: String): List<Pair<Double, Double>> {
        val gpxFile = File(filePath)
        val xmlInputFactory = XMLInputFactory.newInstance()
        val eventReader = xmlInputFactory.createXMLEventReader(FileInputStream(gpxFile))

        val points = mutableListOf<Pair<Double, Double>>()

        var latitude: Double? = null
        var longitude: Double? = null

        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event.isStartElement) {
                when (event.asStartElement().name.localPart) {
                    "trkpt" -> {
                        val latAttr = event.asStartElement().getAttributeByName(QName("lat"))
                        val lonAttr = event.asStartElement().getAttributeByName(QName("lon"))

                        if (latAttr != null && lonAttr != null) {
                            latitude = latAttr.value.toDouble()
                            longitude = lonAttr.value.toDouble()
                        }
                    }
                }
            } else if (event.isEndElement) {
                when (event.asEndElement().name.localPart) {
                    "trkpt" -> {
                        if (latitude != null && longitude != null) {
                            points.add(Pair(longitude, latitude))
                            latitude = null
                            longitude = null
                        }
                    }
                }
            }
        }

        eventReader.close()

        return points
    }


}

data class Tile(val xTile: Int, val yTile: Int, val zoomLevel: Int)

data class LonLat(val lon: Double, val lat: Double)

data class MapBoundaries(val startX: Int, val startY: Int, val endX: Int, val endY: Int)
