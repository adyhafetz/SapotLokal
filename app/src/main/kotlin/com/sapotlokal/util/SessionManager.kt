package com.sapotlokal.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

/**
 * SessionManager — the ONE way to sign out.
 *
 * Why: signing out with only `auth.signOut()` left the previous person's cached name/tier on
 * the device (the next user briefly saw them) and kept the device subscribed to the previous
 * user's shop push topics.
 *
 * What it does:
 *  1. Clears the local user cache.
 *  2. Deletes the FCM token — that drops EVERY topic subscription for this install; a fresh
 *     token is issued automatically for the next user.
 *  3. Signs out of Firebase Auth.
 */
object SessionManager {

    const val PREFS_NAME = "sapot_user_prefs"
    private const val TAG = "SessionManager"

    fun signOut(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()

        try {
            FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener { Log.w(TAG, "Could not delete FCM token on sign-out", it) }
        } catch (e: Exception) {
            Log.w(TAG, "FCM deleteToken threw on sign-out", e)
        }

        FirebaseAuth.getInstance().signOut()
    }
}
