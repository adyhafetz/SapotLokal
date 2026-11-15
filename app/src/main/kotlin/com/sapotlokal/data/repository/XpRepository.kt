package com.sapotlokal.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.AppConfig.Rank
import com.sapotlokal.data.model.XpEvent

/**
 * XpRepository — the customer's XP and rank (REFRACTOR.md §3, §4).
 *
 * XP is never spent and never goes down; the rank comes from it. Every XP award is three paths
 * written together with whatever the award is for:
 *   xpEvents/{uid}/{eventKey}   the event (key is fixed, so the rules let it be written once)
 *   users/{uid}/xp              ServerValue.increment(amount)
 *   users/{uid}/rank            the rank after the award
 */
object XpRepository {

    private val db = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference

    /** A customer's XP and the rank it gives. */
    data class Standing(val xp: Int, val rank: Rank)

    fun standingOf(user: DataSnapshot): Standing {
        val xp = user.child("xp").getValue(Long::class.java)?.toInt() ?: 0
        return Standing(xp, Rank.fromXp(xp))
    }

    /** Reads [uid]'s XP once. Falls back to rank 1 if it can't be read, so screens still draw. */
    fun fetchStanding(uid: String, onResult: (Standing) -> Unit) {
        if (uid.isBlank()) return onResult(Standing(0, Rank.JIRAN_BARU))
        db.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = onResult(standingOf(snapshot))
            override fun onCancelled(error: DatabaseError) = onResult(Standing(0, Rank.JIRAN_BARU))
        })
    }

    /**
     * The paths that award [amount] XP to [uid] for the event [eventKey], given the customer's
     * [current] standing. Add them to the same updateChildren as the thing being rewarded.
     * Returns the paths and the rank after the award.
     */
    fun awardPaths(
        uid: String,
        eventKey: String,
        amount: Int,
        source: String,
        current: Standing,
        now: Long = System.currentTimeMillis()
    ): Pair<Map<String, Any>, Rank> {
        val after = Rank.fromXp(current.xp + amount)
        return mapOf(
            "xpEvents/$uid/$eventKey" to XpEvent(amount = amount, source = source, timestamp = now),
            "users/$uid/xp" to ServerValue.increment(amount.toLong()),
            "users/$uid/rank" to after.level
        ) to after
    }
}
