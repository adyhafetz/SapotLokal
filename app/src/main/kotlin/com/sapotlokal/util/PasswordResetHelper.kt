package com.sapotlokal.util

import android.content.Context
import android.text.InputType
import android.util.Patterns
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

/**
 * Shows a small "Reset password" dialog and asks Firebase Auth to email a reset link.
 *
 * The success message is deliberately the same whether or not the email has an account, so the
 * dialog can't be used to discover which emails are registered.
 */
object PasswordResetHelper {

    fun show(context: Context, prefillEmail: String = "") {
        val density = context.resources.displayMetrics.density
        val pad = (20 * density).toInt()

        val input = EditText(context).apply {
            hint = context.getString(com.sapotlokal.R.string.k_email_address)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(prefillEmail)
            setSelection(text.length)
        }
        val container = FrameLayout(context).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(com.sapotlokal.R.string.k_reset_password))
            .setMessage(context.getString(com.sapotlokal.R.string.k_reset_msg))
            .setView(container)
            .setPositiveButton(context.getString(com.sapotlokal.R.string.k_send_link)) { _, _ ->
                val email = input.text.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, context.getString(com.sapotlokal.R.string.k_valid_email), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        val msg = if (task.isSuccessful || task.exception is
                                com.google.firebase.auth.FirebaseAuthInvalidUserException
                        ) {
                            context.getString(com.sapotlokal.R.string.k_reset_sent)
                        } else {
                            context.getString(com.sapotlokal.R.string.k_reset_failed)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton(context.getString(com.sapotlokal.R.string.k_cancel), null)
            .show()
    }
}
