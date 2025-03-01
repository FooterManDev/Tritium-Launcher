import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.ImageIcon

private val logger = LoggerFactory.getLogger("GlobalFunctions") //TODO: Make a central logger for things like this and others

/**
 * Shortens [Dimension] creation
 */
fun dim(width: Int, height: Int) = Dimension(width, height)

/**
 * Sizes a provided image
 */
fun sizedImg(img: URL?, width: Int, height: Int): ImageIcon {
    val image = ImageIcon(img).image
    val resized = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    return ImageIcon(resized)
}

/**
 * Shortens the [System.getProperty] call
 */
fun getProperty(key: String): String {
    return System.getProperty(key)
}

/**
 * Shortens the [System.getenv] call
 */
fun getEnv(key: String): String? {
    return System.getenv(key)
}

/**
 * Returns File from ~/
 */
fun fromHome(child: String): File {
    return File(getProperty("user.home"), child)
}

/**
 * Returns File from ~/.tritium
 */
fun fromTR(child: String = ""): File {
    return File(fromHome(".tritium"), child)
}

/**
 * Returns the user home directory
 */
fun userHome(): String {
    return getProperty("user.home")
}
val userHome = userHome()

/**
 * Returns the OS name
 */
fun osName(): String {
    return getProperty("os.name").lowercase()
}
val osName = osName()

/**
 * Sends a system notification using Java's SystemTray
 */
suspend fun sendSysNotification(title: String, msg: String) {
    if(SystemTray.isSupported()) {
        try {
            val tray = SystemTray.getSystemTray()
            val img = ImageIcon(withContext(Dispatchers.IO) { // TODO: Move to global val
                ImageIO.read(ClassLoader.getSystemResourceAsStream("images/icon.png"))
            }).image
            val trayIcon = TrayIcon(img, "Tritium")
            trayIcon.isImageAutoSize = true
            tray.add(trayIcon)
            trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO)
            delay(5000)
            tray.remove(trayIcon)
        } catch (e: Exception) {
            logger.error("Error sending system notification: ${e.message}", e)
        }
    } else logger.error("System tray not supported.")
}

/**
 * Loads an image from a URL and optionally scales it to the specified dimensions.
 *
 * @param url the URL of the image to load
 * @param width the desired width of the scaled image (default: null)
 * @param height the desired height of the scaled image (default: null)
 * @return a scaled [ImageIcon], or null if an error occurs during loading
 */
fun loadImage(url: String, width: Int? = null, height: Int? = null, border: Boolean = false): ImageIcon? {
    /*
     * Over the top function for getting the color with the least margin from an image, and setting that color as the image border.
     * Because why not!
     */
    fun getBorderColor(image: Image): Color {
        val buffered = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB)
        val graphics = buffered.graphics
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()

        val pixels = buffered.width * buffered.height
        val redSum = IntArray(pixels)
        val greenSum = IntArray(pixels)
        val blueSum = IntArray(pixels)

        for (x in 0 until buffered.width) {
            for (y in 0 until buffered.height) {
                val rgb = buffered.getRGB(x, y)
                redSum[x + y * buffered.width] = rgb shr 16 and 0xff
                greenSum[x + y * buffered.width] = rgb shr 8 and 0xff
                blueSum[x + y * buffered.width] = rgb and 0xff
            }
        }

        val averageRed = redSum.sum() / pixels
        val averageGreen = greenSum.sum() / pixels
        val averageBlue = blueSum.sum() / pixels

        var maxDistance = 0
        var borderColor: Color? = null

        for (x in 0 until buffered.width) {
            for (y in 0 until buffered.height) {
                val rgb = buffered.getRGB(x, y)
                val red = rgb shr 16 and 0xff
                val green = rgb shr 8 and 0xff
                val blue = rgb and 0xff

                val distance = (red - averageRed) * (red - averageRed) + (green - averageGreen) * (green - averageGreen) + (blue - averageBlue) * (blue - averageBlue)

                if (distance > maxDistance) {
                    maxDistance = distance
                    borderColor = Color(red, green, blue)
                }
            }
        }

        return borderColor!!
    }

    try {
        val original = ImageIO.read(url.toUrl())
        val image = when {
            width == null && height == null -> original
            width != null && height != null -> original.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH)
            width != null -> original.getScaledInstance(width, original.height, BufferedImage.SCALE_SMOOTH)
            else -> original.getScaledInstance(original.width, height!!, BufferedImage.SCALE_SMOOTH)
        }
        if(border) {
            val borderColor = getBorderColor(image)
            val borderImage = BufferedImage(image.getWidth(null) + 2 * 2, image.getHeight(null) + 2 * 2, BufferedImage.TYPE_INT_RGB)
            val graphics = borderImage.graphics
            graphics.color = borderColor
            graphics.fillRect(0, 0, borderImage.width, borderImage.height)
            graphics.drawImage(image, 2, 2, null)
            graphics.dispose()
            return ImageIcon(borderImage)
        } else return ImageIcon(image)
    } catch (e: IOException) {
        logger.error("Error loading image: ${e.message}", e)
        return null
    }
}

/**
 * Simplifies the Insets constructor.
 */
fun insets(all: Int) = Insets(all, all, all, all)

fun roundImage(image: Image, radius: Int): BufferedImage {
    val w = image.getWidth(null)
    val h = image.getHeight(null)
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g2d = img.createGraphics()
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.clip = RoundRectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble(), radius.toDouble(), radius.toDouble())
    g2d.drawImage(image, 0, 0, null)
    g2d.dispose()
    return img
}