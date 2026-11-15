package com.sapotlokal.ui.vendor

import android.graphics.Bitmap
import android.graphics.Color
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.sapotlokal.AppConfig
import com.sapotlokal.data.repository.QrCodeRepository

/**
 * Keeps the rotating shop QR fresh inside the Overview card.
 *  start(): publish a code, show it, and rotate it every QR_ROTATION_SECONDS.
 *  stop():  cancel the timer and DELETE the live code, so nothing can be scanned while the
 *           screen is not visible (the same rule ShowQrActivity follows).
 * The raw value is also shown as a small "Code" line: it is what you type into the customer
 * app's manual box when testing on an emulator without a camera.
 */
class VendorQrController(
    private val vendorId: String,
    private val image: ImageView,
    private val countdown: TextView,
    private val rawValue: TextView
) {
    private var currentQrId: String? = null
    private var timer: CountDownTimer? = null
    private var running = false

    fun start() {
        if (running || vendorId.isBlank()) return
        running = true
        publish()
    }

    fun refreshNow() {
        if (!running) return
        timer?.cancel()
        publish()
    }

    fun stop() {
        running = false
        timer?.cancel()
        currentQrId?.let { QrCodeRepository.deleteCode(it) }
        currentQrId = null
    }

    private fun publish() {
        countdown.text = countdown.context.getString(com.sapotlokal.R.string.kv_generating)
        QrCodeRepository.publishNewCode(
            vendorID = vendorId,
            previousQrId = currentQrId,
            onSuccess = { code ->
                if (!running) {
                    QrCodeRepository.deleteCode(code.qrcodeID)
                    return@publishNewCode
                }
                currentQrId = code.qrcodeID
                image.post {
                    render(code.qrcodeValue)
                    rawValue.text = countdown.context.getString(com.sapotlokal.R.string.kv_code_value, code.qrcodeValue)
                    rawValue.visibility = View.VISIBLE
                    startCountdown()
                }
            },
            onError = { reason -> countdown.post { countdown.text = countdown.context.getString(com.sapotlokal.R.string.kv_error_reason, reason) } }
        )
    }

    private fun startCountdown() {
        timer?.cancel()
        timer = object : CountDownTimer(AppConfig.QR_ROTATION_SECONDS * 1_000L, 1_000L) {
            override fun onTick(ms: Long) {
                countdown.text = countdown.context.getString(com.sapotlokal.R.string.kv_refreshes_in, (ms / 1_000L).toInt())
            }

            override fun onFinish() {
                if (running) publish()
            }
        }.start()
    }

    private fun render(value: String) {
        try {
            val size = 512
            val m = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (m[x, y]) Color.BLACK else Color.WHITE)
            image.setImageBitmap(bmp)
        } catch (e: Exception) {
            countdown.text = countdown.context.getString(com.sapotlokal.R.string.kv_could_not_render)
        }
    }
}
