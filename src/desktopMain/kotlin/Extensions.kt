import java.io.File
import java.net.URI
import java.net.URL

fun String.toUrl(): URL {
    return URI(this).toURL()
}

fun String.toURI(): URI {
    return URI(this)
}

/**
 * Returns File from ~/
 */
fun fromHome(child: String): File {
    return File(System.getProperty("user.home"), child)
}

/**
 * Returns File from ~/.tritium
 */
fun fromTR(child: String): File {
    return File(fromHome(".tritium"), child)
}

/**
 * Returns the user home directory
 */
fun userHome(): String {
    return System.getProperty("user.home")
}
val userHome = userHome()