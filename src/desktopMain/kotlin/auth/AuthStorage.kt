package auth

import java.util.prefs.Preferences

// Not used, probably won't be the token storage. Still testing API.
object AuthStorage {
    private val prefs = Preferences.userRoot().node("TritiumAuth")

    fun saveToken(token: String) {
        prefs.put("auth_token", token)
    }

    fun getToken(): String? {
        return prefs.get("auth_token", null)
    }

    fun clearToken() {
        prefs.remove("auth_token")
    }
}