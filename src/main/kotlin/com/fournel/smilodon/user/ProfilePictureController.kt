package com.fournel.smilodon.user

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletResponse

@Controller
class ProfilePictureController {

    @GetMapping(value = ["/open-api/profile_picture"], produces = [MediaType.IMAGE_PNG_VALUE])
    fun render(@RequestParam("user") user: String, response: HttpServletResponse): ResponseEntity<ByteArray> {
        getProfilePicture(user, response)
        return ResponseEntity.ok().build()
    }

    fun getProfilePicture(user: String, response: HttpServletResponse) {
        val hash = sha1(user)
        val baseColor = hashToColor(hash)
        val imageSize = 200

        val image = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // Set rendering hints for better image quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        // Draw the background gradient
        val gradientPaint = GradientPaint(0f, 0f, baseColor, imageSize.toFloat(), imageSize.toFloat(), baseColor.darker().darker().darker())
        g2d.paint = gradientPaint
        g2d.fillRect(0, 0, imageSize, imageSize)

        // Generate pixels randomly using the hash as seed
        val random = Random(hash.hashCode().toLong())
        for (x in 0 until imageSize) {
            for (y in 0 until imageSize) {
                val randomValue = random.nextFloat()

                val color = Color(
                    (baseColor.red * randomValue * 2.5f).toInt().coerceIn(0, 255),
                    (baseColor.green * randomValue * 2.5f).toInt().coerceIn(0, 255),
                    (baseColor.blue * randomValue * 2.5f).toInt().coerceIn(0, 255)
                )

                image.setRGB(x, y, color.rgb)
            }
        }

        // Clean up resources
        g2d.dispose()

        // Write the image to the response
        response.contentType = "image/png"
        ImageIO.write(image, "png", response.outputStream)
    }




    private fun sha1(input: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(input.toByteArray())
    }

    private fun hashToColor(hash: ByteArray): Color {
        val red = hash[0].toInt() and 0xff
        val green = hash[1].toInt() and 0xff
        val blue = hash[2].toInt() and 0xff
        return Color(red, green, blue)
    }
}
