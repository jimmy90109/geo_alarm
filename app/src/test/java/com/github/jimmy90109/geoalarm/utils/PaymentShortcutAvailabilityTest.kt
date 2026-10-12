package com.github.jimmy90109.geoalarm.utils

import com.github.jimmy90109.geoalarm.data.PaymentShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentShortcutAvailabilityTest {
    @Test
    fun `installedShortcuts returns empty when no supported apps are installed`() {
        val result = PaymentShortcutAvailability.installedShortcuts { false }

        assertTrue(result.isEmpty())
    }

    @Test
    fun `installedShortcuts returns the matching supported app`() {
        val installedPackage = PaymentShortcut.JkoPay.packageName

        val result = PaymentShortcutAvailability.installedShortcuts { packageName ->
            packageName == installedPackage
        }

        assertEquals(listOf(PaymentShortcut.JkoPay), result)
    }

    @Test
    fun `installedShortcuts preserves supported app order`() {
        val installedPackages = setOf(
            PaymentShortcut.IpassMoney.packageName,
            PaymentShortcut.TaiwanPay.packageName,
        )

        val result = PaymentShortcutAvailability.installedShortcuts { packageName ->
            packageName in installedPackages
        }

        assertEquals(
            listOf(PaymentShortcut.IpassMoney, PaymentShortcut.TaiwanPay),
            result,
        )
    }
}
