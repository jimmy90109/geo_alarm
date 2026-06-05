package com.github.jimmy90109.geoalarm.utils

import android.app.Activity
import android.os.Bundle
import com.github.jimmy90109.geoalarm.data.PaymentShortcut

class PaymentShortcutLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openShortcutAndFinish()
    }

    private fun openShortcutAndFinish() {
        PaymentShortcutNotifier.cancel(this)

        val shortcut = PaymentShortcut.fromId(
            intent.getStringExtra(PaymentShortcutNotifier.EXTRA_SHORTCUT_ID),
        )
        if (shortcut != null) {
            startActivity(PaymentShortcutNotifier.createOpenPaymentIntent(this, shortcut))
        }

        finish()
    }
}
