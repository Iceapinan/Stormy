package com.iceapinan.stormy

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.content.Context
/**
 * Created by IceApinan on 22/8/17.
 */
class AlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context : Context = activity
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.error_title))
                .setMessage(context.getString(R.string.error_message))
                .setPositiveButton(context.getString(R.string.error_ok_button_text), null)
        return builder.create()
    }
}