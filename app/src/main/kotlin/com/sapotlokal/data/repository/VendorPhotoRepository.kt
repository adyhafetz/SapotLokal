package com.sapotlokal.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * VendorPhotoRepository — a shop's photo, stored as a small JPEG (Base64 text) at
 * /vendorPhotos/{vendorId}.
 *
 * Why a separate node: /vendors is downloaded in full by several screens (shop list, home,
 * vouchers). Keeping the image apart means those lists stay small and a photo is only fetched
 * when a screen actually needs to draw it. Photos are shrunk to ~800px and under ~120 KB, which
 * keeps this inside the free Firebase plan (no Firebase Storage needed).
 */
object VendorPhotoRepository {

    private const val MAX_SIDE = 800
    private const val MAX_BYTES = 120 * 1024

    private val db get() = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference
    private val cache = object : LruCache<String, Bitmap>(12) {}

    /** Reads the picked image, shrinks it and returns Base64 JPEG text, or null if unreadable. */
    fun prepare(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver

            // Pass 1: measure only, to pick a cheap sample size.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2

            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
            val bmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
            } else decoded

            var quality = 80
            val out = ByteArrayOutputStream()
            do {
                out.reset()
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
                quality -= 10
            } while (out.size() > MAX_BYTES && quality >= 30)

            if (out.size() > MAX_BYTES * 2) null else Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun save(vendorId: String, base64: String, onResult: (Boolean) -> Unit) {
        db.child("vendorPhotos").child(vendorId).setValue(base64)
            .addOnSuccessListener {
                cache.remove(vendorId)
                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }

    fun remove(vendorId: String, onResult: (Boolean) -> Unit = {}) {
        db.child("vendorPhotos").child(vendorId).removeValue()
            .addOnCompleteListener {
                cache.remove(vendorId)
                onResult(it.isSuccessful)
            }
    }

    /** Loads (and caches) a shop photo. Calls back with null when the shop has none. */
    fun load(vendorId: String, onResult: (Bitmap?) -> Unit) {
        if (vendorId.isBlank()) {
            onResult(null)
            return
        }
        cache.get(vendorId)?.let {
            onResult(it)
            return
        }
        db.child("vendorPhotos").child(vendorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val text = snapshot.getValue(String::class.java)
                    val bmp = try {
                        text?.let {
                            val bytes = Base64.decode(it, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } catch (e: Exception) {
                        null
                    }
                    if (bmp != null) cache.put(vendorId, bmp)
                    onResult(bmp)
                }

                override fun onCancelled(error: DatabaseError) = onResult(null)
            })
    }
}
