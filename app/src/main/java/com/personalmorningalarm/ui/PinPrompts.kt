package com.personalmorningalarm.ui

import android.content.Context
import com.personalmorningalarm.R
import com.personalmorningalarm.util.PinManager

/**
 * Builds the [PinDialog] flows for the alarm PIN. Each flow drives the single
 * reusable keypad through its steps and reports the outcome via callbacks.
 */
object PinPrompts {

    /**
     * Runs [action] immediately if no PIN is set; otherwise asks for the PIN and
     * runs [action] only on a correct entry. A wrong PIN is rejected in place;
     * cancelling does nothing.
     */
    fun guard(context: Context, pinManager: PinManager, action: () -> Unit) {
        if (!pinManager.isPinSet()) {
            action()
            return
        }
        PinDialog(
            context = context,
            title = context.getString(R.string.pin_enter_title),
            message = context.getString(R.string.pin_enter_message),
            onComplete = { pin, dialog ->
                if (pinManager.verify(pin)) {
                    dialog.dismiss()
                    action()
                } else {
                    dialog.showError(context.getString(R.string.pin_wrong))
                }
            }
        ).show()
    }

    /** Sets a brand-new PIN: enter once, then re-enter to confirm. */
    fun setup(context: Context, pinManager: PinManager, onResult: (Boolean) -> Unit) {
        var firstEntry: String? = null
        PinDialog(
            context = context,
            title = context.getString(R.string.pin_set_title),
            message = null,
            onComplete = { pin, dialog ->
                val first = firstEntry
                if (first == null) {
                    firstEntry = pin
                    dialog.setHeader(context.getString(R.string.pin_confirm_title), null)
                } else if (pin == first) {
                    pinManager.setPin(pin)
                    dialog.dismiss()
                    onResult(true)
                } else {
                    firstEntry = null
                    dialog.setHeader(context.getString(R.string.pin_set_title), null)
                    dialog.showError(context.getString(R.string.pin_mismatch))
                }
            },
            onCancel = { onResult(false) }
        ).show()
    }

    /** Verifies the current PIN, then runs the set-new-PIN flow. */
    fun change(context: Context, pinManager: PinManager, onResult: (Boolean) -> Unit) {
        PinDialog(
            context = context,
            title = context.getString(R.string.pin_current_title),
            message = null,
            onComplete = { pin, dialog ->
                if (pinManager.verify(pin)) {
                    dialog.dismiss()
                    setup(context, pinManager, onResult)
                } else {
                    dialog.showError(context.getString(R.string.pin_wrong))
                }
            },
            onCancel = { onResult(false) }
        ).show()
    }

    /** Verifies the current PIN, then clears it. */
    fun disable(context: Context, pinManager: PinManager, onResult: (Boolean) -> Unit) {
        PinDialog(
            context = context,
            title = context.getString(R.string.pin_current_title),
            message = null,
            onComplete = { pin, dialog ->
                if (pinManager.verify(pin)) {
                    pinManager.clearPin()
                    dialog.dismiss()
                    onResult(true)
                } else {
                    dialog.showError(context.getString(R.string.pin_wrong))
                }
            },
            onCancel = { onResult(false) }
        ).show()
    }
}
