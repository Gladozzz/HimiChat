package com.jimipurple.himichat.ui.login

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.Window
import androidx.core.widget.doAfterTextChanged
import com.jimipurple.himichat.R
import com.jimipurple.himichat.data.FirebaseSource
import kotlinx.android.synthetic.main.fragment_dialog_reset_password.*

class ResetPasswordDialog(context: Context) : Dialog(context) {

    init {
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.fragment_dialog_reset_password)

        cancelButton.setOnClickListener { cancelButtonOnClick() }
        sendResetRequestButton.setOnClickListener { sendRequestButtonOnClick() }
        this.setOnCancelListener { cancelButtonOnClick() }
        emailEdit.doAfterTextChanged {editable ->
            val email = editable.toString()
            if (editable.isNullOrEmpty()) {
                emailEdit.error = null
            } else if (!isEmailValid(email)) {
                emailEdit.error = context.getString(R.string.invalid_email)
            }
        }
    }

    private fun cancelButtonOnClick() {
        this.cancel()
    }

    private fun sendRequestButtonOnClick() {
        if (!emailEdit.text.isNullOrEmpty()) {
            val fbSource = FirebaseSource(context)
            fbSource.resetPassword(emailEdit.text.toString())
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}