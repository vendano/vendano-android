package net.vendano.vendano_android.data.firebase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import net.vendano.vendano_android.domain.model.Recipient
import net.vendano.vendano_android.util.sha256Base64
import net.vendano.vendano_android.util.hexEncoded
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseProfileService"

/**
 * Firebase Firestore + Storage profile operations.
 * Android equivalent of iOS FirebaseService.swift (profile portions).
 */
@Singleton
class FirebaseProfileService @Inject constructor() {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val storage get() = FirebaseStorage.getInstance().reference

    private val uid get() = auth.currentUser?.uid

    // ─── Public/private state loaders ────────────────────────────

    data class PublicProfile(
        val displayName: String,
        val avatarURL: String?,
        val storeName: String?,
        val walletAddress: String?,
    )

    data class PrivateProfile(
        val phone: List<String>,
        val email: List<String>,
        val viewedFaqIds: List<String>,
    )

    suspend fun fetchPublicProfile(): PublicProfile? {
        val uid = uid ?: return null
        return try {
            val snap = db.collection("public").document(uid).get().await()
            PublicProfile(
                displayName = snap.getString("displayName") ?: "",
                avatarURL = snap.getString("avatarURL"),
                storeName = snap.getString("storeName"),
                walletAddress = snap.getString("walletAddress"),
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchPublicProfile failed: ${e.message}")
            null
        }
    }

    suspend fun fetchPrivateProfile(): PrivateProfile? {
        val uid = uid ?: return null
        return try {
            val snap = db.collection("users").document(uid).get().await()
            PrivateProfile(
                phone = snap.get("phone") as? List<String> ?: emptyList(),
                email = snap.get("email") as? List<String> ?: emptyList(),
                viewedFaqIds = snap.get("viewedFAQ") as? List<String> ?: emptyList(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchPrivateProfile failed: ${e.message}")
            null
        }
    }

    // ─── Profile updates ─────────────────────────────────────────

    suspend fun saveAddress(address: String) {
        val uid = uid ?: return
        db.collection("public").document(uid)
            .set(mapOf("walletAddress" to address), com.google.firebase.firestore.SetOptions.merge())
            .await()
        db.collection("users").document(uid)
            .set(mapOf("walletAddress" to address), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun updateDisplayName(name: String) {
        val uid = uid ?: return
        db.collection("public").document(uid).set(
            mapOf("displayName" to name, "updatedDate" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    suspend fun updateStoreName(storeName: String) {
        val uid = uid ?: return
        db.collection("public").document(uid).set(
            mapOf("storeName" to storeName, "updatedDate" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    // ─── Phone / Email save + remove ─────────────────────────────

    suspend fun savePhone(phone: String) {
        val uid = uid ?: return
        val normalized = phone.filter { it.isDigit() }
        val hash = normalized.sha256Base64()

        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public").document(uid)

        userRef.set(
            mapOf("phone" to FieldValue.arrayUnion(phone), "updatedDate" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        publicRef.set(
            mapOf("phoneHashes" to FieldValue.arrayUnion(hash), "updatedDate" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun saveEmail(email: String) {
        val uid = uid ?: return
        val norm = email.lowercase()
        val hash = norm.sha256Base64()

        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public").document(uid)

        userRef.set(
            mapOf("email" to FieldValue.arrayUnion(email), "updatedDate" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        publicRef.set(
            mapOf("emailHashes" to FieldValue.arrayUnion(hash), "updatedDate" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun removePhone(phone: String) {
        val uid = uid ?: return
        val hash = phone.filter { it.isDigit() }.sha256Base64()
        safeUpdate(
            db.collection("users").document(uid),
            mapOf("phone" to FieldValue.arrayRemove(phone), "updatedDate" to FieldValue.serverTimestamp()),
        )
        safeUpdate(
            db.collection("public").document(uid),
            mapOf("phoneHashes" to FieldValue.arrayRemove(hash), "updatedDate" to FieldValue.serverTimestamp()),
        )
    }

    suspend fun removeEmail(email: String) {
        val uid = uid ?: return
        val hash = email.lowercase().sha256Base64()
        safeUpdate(
            db.collection("users").document(uid),
            mapOf("email" to FieldValue.arrayRemove(email), "updatedDate" to FieldValue.serverTimestamp()),
        )
        safeUpdate(
            db.collection("public").document(uid),
            mapOf("emailHashes" to FieldValue.arrayRemove(hash), "updatedDate" to FieldValue.serverTimestamp()),
        )
    }

    // ─── Recipient lookup ─────────────────────────────────────────

    /**
     * Find a Vendano user by email/phone hash or wallet address.
     * Mirrors iOS FirebaseService.fetchRecipient(for:).
     */
    suspend fun fetchRecipient(handle: String): Recipient? {
        return try {
            val hash = handle.sha256Base64()
            val col = db.collection("public")

            val queries = listOf(
                col.whereArrayContains("emailHashes", hash),
                col.whereArrayContains("phoneHashes", hash),
                col.whereEqualTo("walletAddress", handle),
            )

            for (query in queries) {
                val snap = query.get().await()
                val doc = snap.documents.firstOrNull() ?: continue
                val data = doc.data ?: continue
                val address = data["walletAddress"] as? String ?: continue
                val displayName = (data["displayName"] as? String) ?: ""
                val storeName = (data["storeName"] as? String) ?: ""
                val preferredName = if (storeName.isNotBlank()) storeName else displayName
                val avatar = data["avatarURL"] as? String
                return Recipient(name = preferredName, avatarURL = avatar, address = address)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchRecipient failed: ${e.message}")
            null
        }
    }

    // ─── Avatar ───────────────────────────────────────────────────

    suspend fun uploadAvatar(jpegBytes: ByteArray): String? {
        val uid = uid ?: return null
        return try {
            val thumbBytes = resizeJpeg(jpegBytes, 200)
            val ref = storage.child("avatars/$uid/avatar_thumb.png")
            ref.putBytes(thumbBytes).await()
            val url = ref.downloadUrl.await().toString()

            db.collection("public").document(uid).set(
                mapOf("avatarURL" to url, "updatedDate" to FieldValue.serverTimestamp()),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
            url
        } catch (e: Exception) {
            Log.e(TAG, "uploadAvatar failed: ${e.message}")
            null
        }
    }

    suspend fun fetchAvatarBytes(): ByteArray? {
        val uid = uid ?: return null
        return try {
            val ref = storage.child("avatars/$uid/avatar_thumb.png")
            val maxSize = 500 * 500L
            ref.getBytes(maxSize).await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteAvatarFolder() {
        val uid = uid ?: return
        try {
            val folderRef = storage.child("avatars/$uid")
            folderRef.listAll().await().items.forEach {
                try { it.delete().await() } catch (e: Exception) { Log.w(TAG, "Delete failed: ${it.path}") }
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteAvatarFolder: ${e.message}")
        }
    }

    // ─── FAQ tracking ─────────────────────────────────────────────

    suspend fun markFaqViewed(faqId: String) {
        val uid = uid ?: return
        db.collection("users").document(uid).set(
            mapOf("viewedFAQ" to FieldValue.arrayUnion(faqId)),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    // ─── Pending contacts ─────────────────────────────────────────

    suspend fun addPendingContact(handle: String) {
        val uid = uid ?: return
        val hash = handle.lowercase().sha256Base64()
        safeUpdate(
            db.collection("users").document(uid),
            mapOf("pendingContacts" to FieldValue.arrayUnion(hash), "updatedDate" to FieldValue.serverTimestamp()),
        )
    }

    // ─── Transaction record ───────────────────────────────────────

    suspend fun recordTransaction(recipientAddress: String, amount: Double, txHash: String) {
        val uid = uid ?: return
        try {
            db.collection("transactionRecords").add(
                mapOf(
                    "recipientAddress" to recipientAddress,
                    "amount" to amount,
                    "senderUid" to uid,
                    "txHash" to txHash,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "notificationSent" to false,
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "recordTransaction failed: ${e.message}")
        }
    }

    // ─── FCM token ────────────────────────────────────────────────

    suspend fun setFcmToken(token: String) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).set(
                mapOf("fcmToken" to token),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "setFcmToken failed: ${e.message}")
        }
    }

    // ─── Nuke ─────────────────────────────────────────────────────

    suspend fun removeUserData() {
        val uid = uid ?: return
        try {
            val batch = db.batch()
            val feedbackSnap = db.collection("users").document(uid)
                .collection("feedback").get().await()
            feedbackSnap.documents.forEach { batch.delete(it.reference) }
            batch.delete(db.collection("users").document(uid))
            batch.delete(db.collection("public").document(uid))
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "removeUserData failed: ${e.message}")
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private suspend fun safeUpdate(ref: com.google.firebase.firestore.DocumentReference, data: Map<String, Any>) {
        try { ref.update(data).await() } catch (e: Exception) {
            Log.w(TAG, "safeUpdate on ${ref.path} failed: ${e.message}")
        }
    }

    private fun resizeJpeg(input: ByteArray, targetSize: Int): ByteArray {
        val bmp = BitmapFactory.decodeByteArray(input, 0, input.size)
        val side = minOf(bmp.width, bmp.height)
        val x = (bmp.width - side) / 2
        val y = (bmp.height - side) / 2
        val cropped = Bitmap.createBitmap(bmp, x, y, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return out.toByteArray()
    }
}
