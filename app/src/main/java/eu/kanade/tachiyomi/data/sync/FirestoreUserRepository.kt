package eu.kanade.tachiyomi.data.sync

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user profiles and usernames in Firebase Firestore.
 * All claim/rename/release operations use atomic batch writes.
 */
class FirestoreUserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection(CloudUser.COLLECTION)
    private val usernamesCollection = db.collection(CloudUser.USERNAMES_COLLECTION)

    /**
     * Checks whether a Firestore profile exists for the given UID.
     */
    suspend fun hasProfile(uid: String): Boolean {
        return try {
            val doc = usersCollection.document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepo", "Error checking profile, assuming true to prevent gatekeeper lock", e)
            true
        }
    }

    /**
     * Gets the CloudUser profile for the given UID, or null if not found.
     */
    suspend fun getUser(uid: String): CloudUser? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) doc.toObject(CloudUser::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a username is available (not yet claimed).
     * Returns a tri-state: Available, Taken, or Error (so the UI can distinguish network errors).
     */
    suspend fun checkUsernameAvailability(username: String): UsernameAvailability {
        return try {
            val doc = usernamesCollection.document(username).get().await()
            if (doc.exists()) UsernameAvailability.Taken else UsernameAvailability.Available
        } catch (e: Exception) {
            UsernameAvailability.Error(e.message ?: "Could not check availability")
        }
    }

    /**
     * Creates a new user profile and claims the username atomically.
     * This is used during first-time sign-in (The Gatekeeper).
     *
     * Atomic batch:
     *   1. Create usernames/{username} lock
     *   2. Create users/{uid} profile
     *
     * @return Result.success(CloudUser) or Result.failure(Exception)
     */
    suspend fun createUser(uid: String, username: String): Result<CloudUser> {
        // Validate
        val validationError = CloudUser.validateUsername(username)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val user = CloudUser(
            username = username,
            displayName = username,
            avatarUrl = "",
            bio = "",
            joinedAt = Timestamp.now(),
            usernameChangedAt = null,
        )

        return try {
            val batch = db.batch()

            // 1. Create the username lock
            val usernameRef = usernamesCollection.document(username)
            batch.set(usernameRef, mapOf(
                "uid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
            ))

            // 2. Create the user profile
            val userRef = usersCollection.document(uid)
            batch.set(userRef, mapOf(
                "username" to username,
                "displayName" to username,
                "avatarUrl" to "",
                "bio" to "",
                "joinedAt" to FieldValue.serverTimestamp(),
            ))

            batch.commit().await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Changes a user's username atomically (The Safe Swap).
     *
     * Atomic batch:
     *   1. Delete usernames/{oldUsername} lock
     *   2. Create usernames/{newUsername} lock
     *   3. Update users/{uid} profile
     *
     * @return Result.success(Unit) or Result.failure(Exception)
     */
    suspend fun renameUsername(uid: String, oldUsername: String, newUsername: String): Result<Unit> {
        // Validate
        val validationError = CloudUser.validateUsername(newUsername)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        if (oldUsername == newUsername) {
            return Result.failure(IllegalArgumentException("New username is the same as the current one"))
        }

        return try {
            // Check cooldown from server data first
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(CloudUser::class.java)
            if (user?.usernameChangedAt != null) {
                val elapsedMs = System.currentTimeMillis() - (user.usernameChangedAt.seconds * 1000L)
                if (elapsedMs < CloudUser.USERNAME_CHANGE_COOLDOWN_MS) {
                    val daysLeft = ((CloudUser.USERNAME_CHANGE_COOLDOWN_MS - elapsedMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                    return Result.failure(IllegalStateException("You cannot change your username for another $daysLeft days."))
                }
            }

            val batch = db.batch()

            // 1. Delete old username lock
            val oldUsernameRef = usernamesCollection.document(oldUsername)
            batch.delete(oldUsernameRef)

            // 2. Create new username lock
            val newUsernameRef = usernamesCollection.document(newUsername)
            batch.set(newUsernameRef, mapOf(
                "uid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
            ))

            // 3. Update user profile
            val userRef = usersCollection.document(uid)
            batch.update(userRef, mapOf(
                "username" to newUsername,
                "usernameChangedAt" to FieldValue.serverTimestamp(),
            ))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates profile fields (displayName, bio, avatarUrl) — NOT the username.
     */
    suspend fun updateProfile(
        uid: String,
        displayName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
    ): Result<Unit> {
        val updates = mutableMapOf<String, Any>()
        displayName?.let {
            if (it.length > CloudUser.MAX_DISPLAY_NAME_LENGTH) {
                return Result.failure(IllegalArgumentException("Display name too long (max ${CloudUser.MAX_DISPLAY_NAME_LENGTH})"))
            }
            updates["displayName"] = it
        }
        bio?.let {
            if (it.length > CloudUser.MAX_BIO_LENGTH) {
                return Result.failure(IllegalArgumentException("Bio too long (max ${CloudUser.MAX_BIO_LENGTH})"))
            }
            updates["bio"] = it
        }
        avatarUrl?.let {
            if (it.isNotEmpty() && !it.startsWith("https://")) {
                return Result.failure(IllegalArgumentException("Avatar URL must use HTTPS"))
            }
            updates["avatarUrl"] = it
        }

        if (updates.isEmpty()) return Result.success(Unit)

        return try {
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Searches users by username prefix. Returns up to [limit] results.
     * Uses Firestore range query on the username field.
     */
    suspend fun searchByPrefix(prefix: String, limit: Int = 10): List<Pair<String, CloudUser>> {
        if (prefix.isEmpty()) return emptyList()
        val lowered = prefix.lowercase()
        // Create the upper bound for the range query
        // e.g., "igk" -> search for >= "igk" and < "igl"
        val end = lowered.substring(0, lowered.length - 1) +
            (lowered.last() + 1).toChar()

        return try {
            val snapshot = usersCollection
                .whereGreaterThanOrEqualTo("username", lowered)
                .whereLessThan("username", end)
                .orderBy("username", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(CloudUser::class.java)
                if (user != null) doc.id to user else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Deletes a user account atomically.
     *
     * Atomic batch:
     *   1. Delete usernames/{username} lock
     *   2. Delete users/{uid}/private/settings
     *   3. Delete users/{uid} profile
     *
     * After the batch, call FirebaseAuth.currentUser.delete() separately.
     */
    suspend fun deleteAccount(uid: String, username: String): Result<Unit> {
        return try {
            val batch = db.batch()

            // 1. Delete username lock
            batch.delete(usernamesCollection.document(username))

            // 2. Delete private settings subcollection doc
            batch.delete(
                usersCollection.document(uid)
                    .collection(CloudUser.PRIVATE_COLLECTION)
                    .document(CloudUser.SETTINGS_DOC)
            )

            // 3. Delete user profile
            batch.delete(usersCollection.document(uid))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Tri-state result for username availability checks.
 * Prevents ghost "username taken" errors when the real issue is a network/permission error.
 */
sealed class UsernameAvailability {
    data object Available : UsernameAvailability()
    data object Taken : UsernameAvailability()
    data class Error(val message: String) : UsernameAvailability()
}
