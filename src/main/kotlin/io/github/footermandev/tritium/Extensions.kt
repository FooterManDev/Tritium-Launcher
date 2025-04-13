package io.github.footermandev.tritium

import kotlinx.io.IOException
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.URI
import java.net.URL
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.PI

private val logger = LoggerFactory.getLogger("ExtensionFunctions")

/**
 * Includes various extension functions.
 */

fun String.toUrl(): URL {
    return URI(this).toURL()
}

fun String.toURI(): URI {
    return URI(this)
}

//String to Path
fun String.toPath(): Path {
    return Path.of(this)
}

fun String.shortenHome(): String {
    var index = -1
    repeat(4) {
        index = this.indexOf('/', index + 1)
        if(index == -1) return this
    }

    return "~${this.substring(index)}"
}

fun String.parseColor(): Color {
    return when {
        this.startsWith("#") -> Color.decode(this)
        this.startsWith("0x") -> Color.decode(this)
        this.startsWith("0X") -> Color.decode(this)
        !this.startsWith("#") || !this.startsWith("0x") || !this.startsWith("0X") -> Color.decode("#$this")
        else -> Color.WHITE
    }
}

fun Double.toRadians(): Double = this * (PI / 180.0)

fun Path.toImage(): Image? {
    if (!toFile().exists()) {
        logger.error("Error loading image: File does not exist at path: {}", this)
        return null
    }
    if (!toFile().canRead()) {
        logger.error("Error loading image: File is not readable at path: {}", this)
        return null
    }
    try {
        val image = ImageIO.read(toFile())
        if (image == null) {
            logger.error("Error loading image: ImageIO returned null for path: {}", this)
            return null
        }
        return image
    } catch (e: IOException) {
        logger.error("Error loading image: {} for path: {}", e.message, this, e)
        return null
    } catch (e: Exception) {
        logger.error("Unexpected error loading image: {} for path: {}", e.message, this, e)
        return null
    }
}

fun Path.mkdir(): Boolean {
    return try { this.toFile().mkdir() } catch (e: IOException) { logger.error("Error creating directory: {}", e.message, e); false}
}

fun Path.mkdirs(): Boolean {
    return try { this.toFile().mkdirs() } catch (e: IOException) { logger.error("Error creating directory: {}", e.message, e); false}
}

// Rescales an image to the target dimensions, using high quality interpolation.
fun Image.getHighQualityScaledInstance(targetWidth: Int, targetHeight: Int): Image {
    val sourceW = getWidth(null)
    val sourceH = getHeight(null)

    if (sourceW <= 64 || sourceH <= 64) {
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g2 = scaled.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g2.drawImage(this, 0, 0, targetWidth, targetHeight, null)
        g2.dispose()
        return scaled
    }

    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
    val g2 = scaled.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.drawImage(this, 0, 0, targetWidth, targetHeight, null)
    g2.dispose()
    return scaled
}