package com.sapotlokal.ui.vendor

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.graphics.Bitmap
import android.graphics.Color
import com.sapotlokal.AppConfig
import com.sapotlokal.data.model.QrCode
import com.sapotlokal.data.repository.QrCodeRepository
import com.sapotlokal.databinding.ActivityShowQrBinding

/**
 * ShowQrActivity — Phase 4: Rotating Shop QR Code screen.
 *
 * HOW THE 60-SECOND ROTATION WORKS:
 *   1. On open, [publishAndStartCountdown] calls QrCodeRepository.publishNewCode().
 *      This writes a new /qrcodes record (isActive=true, expiryDate=now+60s) and
 *      marks the previous code inactive — all in one atomic updateChildren() call.
 *   2. A [CountDownTimer] counts down from 60 to 0, updating tvCountdown each second.
 *   3. When the timer hits 0, [publishAndStartCountdown] is called again — the cycle repeats.
 *   4. onStop() cancels the timer and marks the last code inactive so a suspended app
 *      can't leave a stale active code lying around.
 *
 * WHY A NEW DB RECORD EACH ROTATION (not just update the value in place):
 *   The Security Rules on /qrcodes let a vendor write to their own shop's codes.
 *   Keeping each code as a separate record with its own isActive + expiryDate lets
 *   the customer-side validation (QrCodeRepository.validateScannedCode) query by
 *   qrcodeValue without needing to know the record's key in advance.
 *
 * ⚠️ DEBUG: tvDebugValue shows the raw QR string for manual paste into ScanActivity's
 *   debug panel. REMOVE BEFORE PHASE 7 (just set visibility=gone is not enough —
 *   delete the whole debug block).
 */
class ShowQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowQrBinding

    private var vendorID: String = ""
    private var currentQrId: String? = null   // Key of the most recent QRCODE record
    private var countDownTimer: CountDownTimer? = null
    private var needsFreshCode = false   // true after onStop removed the code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        vendorID = intent.getStringExtra(VendorHomeActivity.EXTRA_VENDOR_ID) ?: ""
        if (vendorID.isEmpty()) { finish(); return }

        binding.btnRefreshNow.setOnClickListener {
            countDownTimer?.cancel()
            publishAndStartCountdown()
        }

        publishAndStartCountdown()
    }

    // ── QR publishing + countdown loop ────────────────────────────────────────

    private fun publishAndStartCountdown() {
        binding.tvCountdown.text = getString(com.sapotlokal.R.string.kv_generating)
        binding.ivQrCode.setImageBitmap(null)

        QrCodeRepository.publishNewCode(
            vendorID    = vendorID,
            previousQrId = currentQrId,
            onSuccess   = { newCode ->
                currentQrId = newCode.qrcodeID
                runOnUiThread {
                    renderQrBitmap(newCode.qrcodeValue)
                    // ⚠️ DEBUG — shows raw value so testers can paste it in ScanActivity
                    binding.tvDebugValue.text = "⚠️ DEBUG value: ${newCode.qrcodeValue}"
                    binding.tvDebugValue.visibility = View.VISIBLE
                    startCountdown()
                }
            },
            onError = { reason ->
                runOnUiThread {
                    binding.tvCountdown.text = getString(com.sapotlokal.R.string.kv_error_reason, reason)
                }
            }
        )
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(
            AppConfig.QR_ROTATION_SECONDS * 1_000L,
            1_000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1_000L).toInt()
                binding.tvCountdown.text = getString(com.sapotlokal.R.string.kv_refreshes_in, secs)
            }
            override fun onFinish() {
                publishAndStartCountdown()
            }
        }.start()
    }

    // ── ZXing QR rendering ────────────────────────────────────────────────────

    private fun renderQrBitmap(value: String) {
        try {
            val size    = 512   // px — will be scaled by the ImageView
            val matrix: BitMatrix = MultiFormatWriter().encode(
                value, BarcodeFormat.QR_CODE, size, size
            )
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            binding.tvCountdown.text = getString(com.sapotlokal.R.string.kv_could_not_render)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStop() {
        super.onStop()
        countDownTimer?.cancel()
        // Remove the current code so nothing is scannable while the screen is in the background.
        // (The old version published a replacement "throwaway" code, which stayed VALID.)
        currentQrId?.let { id ->
            QrCodeRepository.deleteCode(id)
            currentQrId = null
            needsFreshCode = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the background: publish a fresh code.
        if (needsFreshCode) {
            needsFreshCode = false
            publishAndStartCountdown()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
