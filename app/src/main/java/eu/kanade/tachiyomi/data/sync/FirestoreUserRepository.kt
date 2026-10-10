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
     * Returns null on network error (so the caller can retry instead of bypassing).
     */
    suspend fun hasProfile(uid: String): Boolean? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepo", "Error checking profile", e)
            null // Return null so caller knows it was a network error, not a real "has profile"
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
        // Use Firestore's standard Unicode sentinel for prefix search
        // \uf8ff is a very high Unicode code point — any string starting with `lowered` will be < lowered + \uf8ff

        return try {
            val snapshot = usersCollection
                .whereGreaterThanOrEqualTo("username", lowered)
                .whereLessThanOrEqualTo("username", lowered + "\uf8ff")
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
     * Saves the user's Top 5 Manga favorites to Firestore.
     * Stores as a list of maps under the "topManga" field.
     */
    suspend fun saveTopManga(uid: String, favorites: List<FavoriteManga>): Result<Unit> {
        return try {
            val data = favorites.mapIndexed { index, fav ->
                mapOf(
                    "title" to fav.title,
                    "author" to fav.author,
                    "description" to fav.description,
                    "thumbnailUrl" to fav.thumbnailUrl,
                    "genres" to fav.genres,
                    "sourceName" to fav.sourceName,
                    "sourceId" to fav.sourceId,
                    "mangaUrl" to fav.mangaUrl,
                    "status" to fav.status,
                    "order" to index,
                )
            }
            usersCollection.document(uid).update("topManga", data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the user's Top 5 Manga favorites from Firestore.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun getTopManga(uid: String): List<FavoriteManga> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val list = doc.get("topManga") as? List<Map<String, Any?>> ?: return emptyList()
            list.map { map ->
                FavoriteManga(
                    title = map["title"] as? String ?: "",
                    author = map["author"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                    genres = (map["genres"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    sourceName = map["sourceName"] as? String ?: "",
                    sourceId = (map["sourceId"] as? Number)?.toLong() ?: 0L,
                    mangaUrl = map["mangaUrl"] as? String ?: "",
                    status = (map["status"] as? Number)?.toLong() ?: 0L,
                    order = (map["order"] as? Number)?.toInt() ?: 0,
                )
            }.sortedBy { it.order }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Saves the user's Top 5 Novel favorites to Firestore.
     */
    suspend fun saveTopNovels(uid: String, favorites: List<FavoriteManga>): Result<Unit> {
        return try {
            val data = favorites.mapIndexed { index, fav ->
                mapOf(
                    "title" to fav.title,
                    "author" to fav.author,
                    "description" to fav.description,
                    "thumbnailUrl" to fav.thumbnailUrl,
                    "genres" to fav.genres,
                    "sourceName" to fav.sourceName,
                    "sourceId" to fav.sourceId,
                    "mangaUrl" to fav.mangaUrl,
                    "status" to fav.status,
                    "order" to index,
                )
            }
            usersCollection.document(uid).update("topNovels", data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the user's Top 5 Novel favorites from Firestore.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun getTopNovels(uid: String): List<FavoriteManga> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val list = doc.get("topNovels") as? List<Map<String, Any?>> ?: return emptyList()
            list.map { map ->
                FavoriteManga(
                    title = map["title"] as? String ?: "",
                    author = map["author"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                    genres = (map["genres"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    sourceName = map["sourceName"] as? String ?: "",
                    sourceId = (map["sourceId"] as? Number)?.toLong() ?: 0L,
                    mangaUrl = map["mangaUrl"] as? String ?: "",
                    status = (map["status"] as? Number)?.toLong() ?: 0L,
                    order = (map["order"] as? Number)?.toInt() ?: 0,
                )
            }.sortedBy { it.order }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Deletes a user account atomically.
     *
     * Atomic batch:
     *   1. Delete Firebase Auth account (so user is immediately logged out)
     *   2. Delete usernames/{username} lock
     *   3. Delete users/{uid}/private/settings
     *   4. Delete users/{uid} profile
     *
     * Auth is deleted first so if the app crashes mid-deletion, the user is
     * safely logged out. Orphaned Firestore data is harmless.
     */
    suspend fun deleteAccount(uid: String, username: String): Result<Unit> {
        return try {
            // 1. Delete Firebase Auth first (immediate logout = safe state on crash)
            val authUser = FirebaseAuth.getInstance().currentUser
            authUser?.delete()?.await()

            // 2. Clean up Firestore data atomically
            val batch = db.batch()
            batch.delete(usernamesCollection.document(username))
            batch.delete(
                usersCollection.document(uid)
                    .collection(CloudUser.PRIVATE_COLLECTION)
                    .document(CloudUser.SETTINGS_DOC)
            )
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
