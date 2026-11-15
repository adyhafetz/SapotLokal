package com.sapotlokal.util

import android.content.Context

/**
 * FollowedShops — the shops this customer wants "now OPEN / CLOSED" alerts from.
 *
 * A shop becomes "followed" when the customer opens its page or earns points there (the same
 * moments the app subscribes the device to the shop's push topic). Stored locally in the same
 * prefs file SessionManager wipes on logout, so one user's follows never leak to the next.
 */
object FollowedShops {
    private const val KEY = "followed_vendor_ids"

    fun follow(context: Context, vendorId: String) {
        if (vendorId.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(SessionManager.PREFS_NAME, Context.MODE_PRIVATE)
        val updated = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()) + vendorId
        prefs.edit().putStringSet(KEY, updated.toSet()).apply()
    }

    fun all(context: Context): Set<String> =
        context.applicationContext.getSharedPreferences(SessionManager.PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())?.toSet() ?: emptySet()
}
