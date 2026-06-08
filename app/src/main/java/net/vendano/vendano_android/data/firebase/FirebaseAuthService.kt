package net.vendano.vendano_android.data.firebase

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.vendano.vendano_android.data.prefs.AppPreferences
import net.vendano.vendano_android.domain.model.OnboardingStep
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseAuthService"

/**
 * Firebase Authentication service.
 * Android equivalent of iOS FirebaseService.swift (auth portions).
 *
 * Handles:
 *  - Phone OTP (SMS code) via PhoneAuthProvider
 *  - Email magic link via sendSignInLinkToEmail + confirmEmailLink
 *  - User state transitions → OnboardingStep
 */
@Singleton
class FirebaseAuthService @Inject constructor(
    private val prefs: AppPreferences,
) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isSignedIn: Boolean get() = currentUser != null

    // Dedicated IO scope for fire-and-forget DataStore writes inside callbacks
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // ─── Phone OTP ────────────────────────────────────────────────

    /**
     * Trigger an SMS verification for the given E.164 phone number.
     * The verificationId is stored in DataStore for [confirmPhoneOtp].
     */
    suspend fun sendPhoneOtp(
        e164: String,
        activity: android.app.Activity,
        onCodeSent: () -> Unit,
        onError: (String) -> Unit,
    ) {
        prefs.setPhoneNumber(e164)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification (rare on Android) – sign in is handled automatically
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Phone verification failed: ${e.message}")
                onError(e.localizedMessage ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                // Persist the verificationId using a fire-and-forget coroutine
                ioScope.launch {
                    prefs.setPhoneVid(verificationId)
                }
                onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Confirm the 6-digit SMS code.
     * Returns null on success, or an error string.
     */
    suspend fun confirmPhoneOtp(code: String, phoneNumber: String): String? {
        return try {
            val vid = prefs.phoneVid.first() ?: return "Missing verification ID"
            val credential = PhoneAuthProvider.getCredential(vid, code)
            val user = auth.currentUser
            if (user != null) {
                user.linkWithCredential(credential).await()
            } else {
                val result = auth.signInWithCredential(credential).await()
                Log.d(TAG, "Phone sign-in: uid=${result.user?.uid}")
            }
            null // success
        } catch (e: Exception) {
            Log.e(TAG, "confirmPhoneOtp failed: ${e.message}")
            e.localizedMessage ?: "Verification failed"
        }
    }

    // ─── Email magic link ─────────────────────────────────────────

    suspend fun sendEmailLink(email: String): String? {
        return try {
            val settings = ActionCodeSettings.newBuilder()
                .setHandleCodeInApp(true)
                .setUrl("https://signin.vendano.net/welcome")
                .setAndroidPackageName("net.vendano.vendano_android", true, null)
                .build()

            auth.sendSignInLinkToEmail(email, settings).await()
            prefs.setEmailForLink(email)
            null
        } catch (e: Exception) {
            Log.e(TAG, "sendEmailLink failed: ${e.message}")
            e.localizedMessage ?: "Failed to send link"
        }
    }

    suspend fun confirmEmailLink(link: String): String? {
        return try {
            val email = prefs.emailForLink.first() ?: return "Missing email"
            if (!auth.isSignInWithEmailLink(link)) return "Invalid link"

            val credential = EmailAuthProvider.getCredentialWithLink(email, link)
            val user = auth.currentUser
            if (user != null) {
                try {
                    user.linkWithCredential(credential).await()
                } catch (e: FirebaseAuthUserCollisionException) {
                    Log.d(TAG, "Email already linked, ignoring: ${e.message}")
                }
            } else {
                val result = auth.signInWithEmailLink(email, link).await()
                Log.d(TAG, "Email link sign-in: uid=${result.user?.uid}")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "confirmEmailLink failed: ${e.message}")
            e.localizedMessage ?: "Link confirmation failed"
        }
    }

    // ─── User status ──────────────────────────────────────────────

    /**
     * Determine the appropriate OnboardingStep for the current user.
     * Mirrors iOS FirebaseService.getUserStatus().
     */
    suspend fun getUserStatus(isDemo: Boolean): OnboardingStep {
        if (isDemo) return OnboardingStep.WALLET_CHOICE
        val user = auth.currentUser ?: return OnboardingStep.AUTH
        val db = FirebaseFirestore.getInstance()
        val uid = user.uid

        return try {
            val pubSnap = db.collection("public").document(uid).get().await()
            val usrSnap = db.collection("users").document(uid).get().await()

            val hasName = pubSnap.getString("displayName")?.isNotBlank() == true
            val emails = usrSnap.get("email") as? List<*> ?: emptyList<Any>()
            val phones = usrSnap.get("phone") as? List<*> ?: emptyList<Any>()
            val hasHandle = emails.isNotEmpty() || phones.isNotEmpty()

            when {
                !hasHandle -> OnboardingStep.AUTH
                !hasName -> OnboardingStep.PROFILE
                else -> OnboardingStep.WALLET_CHOICE
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserStatus failed: ${e.message}")
            OnboardingStep.AUTH
        }
    }

    // ─── Sign out / Delete ────────────────────────────────────────

    /**
     * Signs the current user out of Firebase without affecting the account.
     * To permanently delete the account, call [deleteAccount] explicitly.
     */
    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign-out failed: ${e.message}")
        }
    }

    /**
     * Permanently deletes the Firebase account. This is irreversible — only call
     * from an explicit "Delete account" flow, never from a normal sign-out path.
     */
    suspend fun deleteAccount() {
        try {
            auth.currentUser?.delete()?.await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete account failed: ${e.message}")
        }
        signOut()
    }

    // ─── Unlink handles ───────────────────────────────────────────

    suspend fun unlinkEmail(email: String): String? {
        return try {
            val user = auth.currentUser ?: return "Not signed in"
            val provider = user.providerData.firstOrNull {
                it.providerId == EmailAuthProvider.PROVIDER_ID &&
                    it.email?.lowercase() == email.lowercase()
            } ?: return null

            if (user.providerData.size <= 1) return "Cannot remove last provider"
            user.unlink(provider.providerId).await()
            null
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    suspend fun unlinkPhone(phone: String): String? {
        return try {
            val user = auth.currentUser ?: return "Not signed in"
            val provider = user.providerData.firstOrNull {
                it.providerId == PhoneAuthProvider.PROVIDER_ID &&
                    it.phoneNumber == phone
            } ?: return null

            if (user.providerData.size <= 1) return "Cannot remove last provider"
            user.unlink(provider.providerId).await()
            null
        } catch (e: Exception) {
            e.localizedMessage
        }
    }
}
