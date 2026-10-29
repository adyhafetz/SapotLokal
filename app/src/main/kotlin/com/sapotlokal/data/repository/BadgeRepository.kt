package com.sapotlokal.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.sapotlokal.AppConfig
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.util.BadgeDialogHelper
import com.sapotlokal.util.SoundManager

/**
 * BadgeRepository — Firebase dispatcher and state checker for user gamification badges.
 *
 * Badges are stored at: /users/{userId}/unlockedBadges/{badgeId} = true
 */
object BadgeRepository {

    private const val TAG = "BadgeRepository"

    private val db = FirebaseDatabase
        .getInstance(AppConfig.DATABASE_URL)
        .reference

    /**
     * Reads /users/{userId}/unlockedBadges/{badge.id}.
     * If false or absent: writes true to Firebase, plays audio cue,
     * and triggers the animated modal dialog via BadgeDialogHelper.
     * If already unlocked: does not re-show modal.
     */
    fun unlockBadge(
        activity: Activity,
        userId: String,
        badge: BadgeDef,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (userId.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val badgeRef = db.child("users").child(userId).child("unlockedBadges").child(badge.id)
        badgeRef.get().addOnSuccessListener { snapshot ->
            val alreadyUnlocked = snapshot.getValue(Boolean::class.java) ?: false
            if (!alreadyUnlocked) {
                badgeRef.setValue(true).addOnSuccessListener {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        activity.runOnUiThread {
                            SoundManager.getInstance(activity).playKaching()
                            BadgeDialogHelper.show(activity, badge)
                        }
                    }
                    onComplete?.invoke(true)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to write badge unlock to Firebase: ${badge.id}", e)
                    onComplete?.invoke(false)
                }
            } else {
                // Already unlocked - do not re-show dialog
                onComplete?.invoke(false)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to check badge status for: ${badge.id}", e)
            onComplete?.invoke(false)
        }
    }

    /**
     * Checks if a user has unlocked a specific badge.
     */
    fun isBadgeUnlocked(
        userId: String,
        badgeId: String,
        onResult: (Boolean) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(false)
            return
        }
        db.child("users").child(userId).child("unlockedBadges").child(badgeId)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.getValue(Boolean::class.java) ?: false)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    /**
     * Fetches all unlocked badges for a user.
     */
    fun getUnlockedBadges(
        userId: String,
        onResult: (Map<String, Boolean>) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(emptyMap())
            return
        }
        db.child("users").child(userId).child("unlockedBadges")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Boolean>()
                snapshot.children.forEach { child ->
                    val key = child.key
                    val value = child.getValue(Boolean::class.java) ?: false
                    if (key != null) {
                        result[key] = value
                    }
                }
                onResult(result)
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }
}

