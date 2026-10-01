package eu.kanade.tachiyomi.data.sync

import androidx.annotation.Keep
import com.google.firebase.Timestamp

/**
 * Represents a user's public profile stored in Firebase Firestore.
 * Maps to the `users/{uid}` document in the cloud database.
 */
@Keep
data class CloudUser(
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val joinedAt: Timestamp? = null,
    val usernameChangedAt: Timestamp? = null,
) {
    companion object {
        const val COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val PRIVATE_COLLECTION = "private"
        const val SETTINGS_DOC = "settings"

        /** Username validation regex: lowercase letter first, then lowercase letters/digits/underscores, 3-20 chars total. */
        val USERNAME_REGEX = Regex("^[a-z][a-z0-9_]{2,19}$")

        /** Reserved usernames that cannot be claimed. */
        val RESERVED_USERNAMES = setOf(
            "admin", "mod", "moderator", "wammy", "system",
            "null", "undefined", "support", "help", "official",
        )

        /** Cooldown period for username changes in milliseconds (14 days). */
        const val USERNAME_CHANGE_COOLDOWN_MS = 14L * 24 * 60 * 60 * 1000

        /** Maximum lengths for profile fields. */
        const val MAX_DISPLAY_NAME_LENGTH = 40
        const val MAX_BIO_LENGTH = 150

        /**
         * Validates a username against format rules and reserved list.
         * Returns null if valid, or an error message string if invalid.
         */
        fun validateUsername(username: String): String? {
            return when {
                username.length < 3 -> "Username must be at least 3 characters"
                username.length > 20 -> "Username must be 20 characters or less"
                !USERNAME_REGEX.matches(username) -> "Only lowercase letters, numbers, and underscores allowed. Must start with a letter"
                username in RESERVED_USERNAMES -> "This username is reserved"
                else -> null
            }
        }
    }
}
