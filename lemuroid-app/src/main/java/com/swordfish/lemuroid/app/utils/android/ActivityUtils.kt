package com.swordfish.lemuroid.app.utils.android

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AlertDialog

// TODO COMPOSE... How do they look in the post compose world?
fun Activity.displayErrorDialog(
    messageId: Int,
    actionLabelId: Int,
    action: () -> Unit,
) {
    displayErrorDialog(resources.getString(messageId), resources.getString(actionLabelId), action)
}

fun Activity.displayErrorDialog(
    message: String,
    actionLabel: String,
    action: () -> Unit,
) {
    // Guard: prevent BadTokenException if Activity is no longer valid
    if (this.isFinishing || this.isDestroyed) {
        Log.w("ActivityUtils", "Skipping error dialog on finishing/destroyed activity: $message")
        return
    }

    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton(actionLabel) { _, _ -> action() }
        .setCancelable(false)
        .show()
}
