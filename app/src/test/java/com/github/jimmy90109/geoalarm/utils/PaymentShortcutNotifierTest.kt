package com.github.jimmy90109.geoalarm.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentShortcutNotifierTest {
    @Test
    fun `planOpenPaymentTarget uses launch app when app is installed`() {
        val result = PaymentShortcutNotifier.planOpenPaymentTarget(
            hasLaunchIntent = true,
            canHandleMarketIntent = true,
        )

        assertEquals(PaymentShortcutNotifier.OpenTarget.LaunchApp, result)
    }

    @Test
    fun `planOpenPaymentTarget uses market when app is not installed`() {
        val result = PaymentShortcutNotifier.planOpenPaymentTarget(
            hasLaunchIntent = false,
            canHandleMarketIntent = true,
        )

        assertEquals(PaymentShortcutNotifier.OpenTarget.Market, result)
    }

    @Test
    fun `planOpenPaymentTarget falls back to web when market is unavailable`() {
        val result = PaymentShortcutNotifier.planOpenPaymentTarget(
            hasLaunchIntent = false,
            canHandleMarketIntent = false,
        )

        assertEquals(PaymentShortcutNotifier.OpenTarget.Web, result)
    }
}
