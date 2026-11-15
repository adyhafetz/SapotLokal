package com.sapotlokal.ui.vendor

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.VendorPhotoRepository
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.databinding.ActivityShopProfileBinding
import com.sapotlokal.ui.auth.LoginActivity
import com.sapotlokal.util.SessionManager

/**
 * ShopProfileActivity — the vendor "Shop profile" tab.
 * Shop photo (shown to customers in the shops carousel), shop details, and the account actions:
 * change password, log out and delete account.
 */
class ShopProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private var vendorID = ""
    private var vendorListener: ValueEventListener? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val base64 = VendorPhotoRepository.prepare(this, uri)
        if (base64 == null) {
            Toast.makeText(this, getString(com.sapotlokal.R.string.kv_bad_image), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        VendorPhotoRepository.save(vendorID, base64) { ok ->
            if (ok) {
                loadPhoto()
                Toast.makeText(this, getString(com.sapotlokal.R.string.kv_photo_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(com.sapotlokal.R.string.kv_photo_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.sapotlokal.util.AppLanguage.bindToggle(binding.languageToggle.root)

        vendorID = intent.getStringExtra(VendorHomeActivity.EXTRA_VENDOR_ID).orEmpty()
        if (vendorID.isBlank()) {
            finish()
            return
        }

        VendorNav.bind(this, binding.vendorNav, VendorNav.Tab.PROFILE) { vendorID }
        binding.photoFrame.clipToOutline = true
        binding.ivShopPhoto.clipToOutline = true

        binding.photoFrame.setOnClickListener { pickPhoto.launch("image/*") }
        binding.btnChangePhoto.setOnClickListener { pickPhoto.launch("image/*") }
        binding.btnUpdateProfile.setOnClickListener {
            startActivity(
                Intent(this, ShopSettingsActivity::class.java)
                    .putExtra(VendorHomeActivity.EXTRA_VENDOR_ID, vendorID)
            )
        }
        binding.rowChangePassword.setOnClickListener { showChangePassword() }
        binding.rowLogout.setOnClickListener { confirmLogout() }
        binding.rowDeleteAccount.setOnClickListener { showDeleteAccount() }

        vendorListener = VendorRepository.listenToVendor(
            vendorID = vendorID,
            onUpdate = { v -> if (v != null) bind(v) },
            onError = { }
        )
        loadPhoto()
    }

    override fun onDestroy() {
        super.onDestroy()
        vendorListener?.let { VendorRepository.removeVendorListener(vendorID, it) }
        vendorListener = null
    }

    private fun bind(v: Vendor) {
        binding.tvProfileName.text = v.vendorName
        binding.tvProfileCategory.text = com.sapotlokal.data.model.ShopCategory.label(this, v.vendorCategory)
        binding.tvDetailCategory.text = com.sapotlokal.data.model.ShopCategory.label(this, v.vendorCategory).ifBlank { getString(com.sapotlokal.R.string.kv_not_set) }
        binding.tvDetailAddress.text = v.vendorAddress.ifBlank { getString(com.sapotlokal.R.string.kv_not_set) }
        binding.tvDetailHours.text = v.operatingHours.ifBlank { getString(com.sapotlokal.R.string.kv_not_set) }
        binding.tvDetailSsm.text = v.ssmNumber.ifBlank { getString(com.sapotlokal.R.string.kv_not_set) }
    }

    private fun loadPhoto() {
        VendorPhotoRepository.load(vendorID) { bmp ->
            if (isFinishing || isDestroyed) return@load
            if (bmp != null) {
                binding.ivShopPhoto.setImageBitmap(bmp)
                binding.photoPlaceholder.visibility = View.GONE
                binding.btnChangePhoto.text = getString(com.sapotlokal.R.string.kv_change_photo)
            } else {
                binding.ivShopPhoto.setImageDrawable(null)
                binding.photoPlaceholder.visibility = View.VISIBLE
                binding.btnChangePhoto.text = getString(com.sapotlokal.R.string.kv_add_photo)
            }
        }
    }

    // ── Account actions ───────────────────────────────────────────────────────

    private fun passwordField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun formOf(vararg fields: EditText): LinearLayout {
        val pad = (20 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            fields.forEach { addView(it) }
        }
    }

    private fun showChangePassword() {
        val current = passwordField(getString(com.sapotlokal.R.string.k_current_password))
        val newer = passwordField(getString(com.sapotlokal.R.string.k_new_password))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.sapotlokal.R.string.k_change_password))
            .setView(formOf(current, newer))
            .setPositiveButton(getString(com.sapotlokal.R.string.k_save)) { _, _ ->
                val newPw = newer.text.toString()
                if (newPw.length < 6) {
                    toast(getString(com.sapotlokal.R.string.k_new_password_short))
                    return@setPositiveButton
                }
                reauthenticate(current.text.toString()) {
                    auth.currentUser?.updatePassword(newPw)
                        ?.addOnSuccessListener { toast(getString(com.sapotlokal.R.string.k_password_changed)) }
                        ?.addOnFailureListener { toast(getString(com.sapotlokal.R.string.k_password_change_failed)) }
                }
            }
            .setNegativeButton(getString(com.sapotlokal.R.string.k_cancel), null)
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.sapotlokal.R.string.k_log_out_q))
            .setPositiveButton(getString(com.sapotlokal.R.string.k_log_out)) { _, _ ->
                SessionManager.signOut(this)
                goToLogin()
            }
            .setNegativeButton(getString(com.sapotlokal.R.string.k_cancel), null)
            .show()
    }

    private fun showDeleteAccount() {
        val password = passwordField(getString(com.sapotlokal.R.string.k_your_password))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.sapotlokal.R.string.k_delete_account_q))
            .setMessage(getString(com.sapotlokal.R.string.kv_delete_shop_msg))
            .setView(formOf(password))
            .setPositiveButton(getString(com.sapotlokal.R.string.k_delete)) { _, _ ->
                reauthenticate(password.text.toString()) { deleteEverything() }
            }
            .setNegativeButton(getString(com.sapotlokal.R.string.k_cancel), null)
            .show()
    }

    /** Firebase only allows sensitive changes shortly after a fresh sign-in, so confirm the password first. */
    private fun reauthenticate(password: String, onSuccess: () -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank() || password.isEmpty()) {
            toast(getString(com.sapotlokal.R.string.k_enter_password))
            return
        }
        user.reauthenticate(EmailAuthProvider.getCredential(email, password))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { toast(getString(com.sapotlokal.R.string.k_password_wrong)) }
    }

    private fun deleteEverything() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val db = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference

        db.child("vouchers").orderByChild("vendorID").equalTo(vendorID).get()
            .addOnSuccessListener { rules ->
                // Remove the shop, its photo and all its reward rules in one atomic write.
                val removals = hashMapOf<String, Any?>(
                    "vendors/$vendorID" to null,
                    "vendorPhotos/$vendorID" to null
                )
                rules.children.forEach { removals["vouchers/${it.key}"] = null }

                db.updateChildren(removals)
                    .addOnSuccessListener {
                        db.child("users").child(uid).removeValue().addOnCompleteListener {
                            user.delete()
                                .addOnSuccessListener {
                                    SessionManager.signOut(this)
                                    goToLogin()
                                }
                                .addOnFailureListener {
                                    toast(getString(com.sapotlokal.R.string.k_login_not_deleted))
                                }
                        }
                    }
                    .addOnFailureListener { toast(getString(com.sapotlokal.R.string.kv_shop_data_not_deleted)) }
            }
            .addOnFailureListener { toast(getString(com.sapotlokal.R.string.kv_server_unreachable)) }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
