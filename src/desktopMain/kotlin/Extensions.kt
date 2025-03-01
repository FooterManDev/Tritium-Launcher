import java.net.URI
import java.net.URL

/**
 * Includes various extension functions.
 */

fun String.toUrl(): URL {
    return URI(this).toURL()
}

fun String.toURI(): URI {
    return URI(this)
}