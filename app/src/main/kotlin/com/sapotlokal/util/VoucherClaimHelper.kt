package com.sapotlokal.util

import android.app.Activity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.sapotlokal.AppConfig
import com.sapotlokal.R
import com.sapotlokal.data.model.Voucher
import com.sapotlokal.data.repository.VoucherRepository
import com.sapotlokal.databinding.DialogClaimVoucherBinding

/**
 * Claim flow shared by the Vouchers page and the shop page: show the reward's details (shop, name,
 * cost, terms, validity), spend the points when the customer taps Claim, and tell them what happened.
 * The voucher then shows up on the Vouchers page with its own QR code.
 */
object VoucherClaimHelper {

    fun confirmAndClaim(
        activity: Activity,
        userID: String,
        rule: Voucher,
        shopName: String,
        onDone: (success: Boolean) -> Unit = {}
    ) {
        val b = DialogClaimVoucherBinding.inflate(LayoutInflater.from(activity))
        val dialog = AlertDialog.Builder(activity).setView(b.root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        b.tvClaimShop.text = shopName
        b.tvClaimTitle.text = rule.voucherName
        b.tvClaimPoints.text = activity.getString(R.string.k_points_n, rule.pointsRequired)
        b.tvClaimTerms.text = rule.voucherDescription.ifBlank { activity.getString(R.string.k_no_terms) }
        b.tvClaimValidity.text = activity.getString(R.string.k_valid_days_after_claim, AppConfig.VOUCHER_EXPIRY_DAYS)

        b.btnClaimCancel.setOnClickListener { dialog.dismiss() }
        b.btnClaimConfirm.setOnClickListener {
            // One tap only: switch the button off so a double tap can't spend the points twice.
            b.btnClaimConfirm.isEnabled = false
            VoucherRepository.claimVoucher(userID, rule) { result ->
                activity.runOnUiThread {
                    dialog.dismiss()
                    when (result) {
                        is VoucherRepository.ClaimResult.Success -> {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.k_claimed_toast, shopName),
                                Toast.LENGTH_LONG
                            ).show()
                            onDone(true)
                        }
                        is VoucherRepository.ClaimResult.Failed -> {
                            Toast.makeText(activity, result.reason, Toast.LENGTH_LONG).show()
                            onDone(false)
                        }
                    }
                }
            }
        }
        dialog.show()
    }
}
