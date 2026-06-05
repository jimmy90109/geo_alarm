package com.github.jimmy90109.geoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentShortcutTest {
    @Test
    fun `fromId returns matching shortcut`() {
        assertEquals(PaymentShortcut.EasyWallet, PaymentShortcut.fromId("easy_wallet"))
        assertEquals(PaymentShortcut.JkoPay, PaymentShortcut.fromId("jkopay"))
        assertEquals(PaymentShortcut.IpassMoney, PaymentShortcut.fromId("ipass_money"))
        assertEquals(PaymentShortcut.IcashPay, PaymentShortcut.fromId("icash_pay"))
        assertEquals(PaymentShortcut.PxPayPlus, PaymentShortcut.fromId("pxpay_plus"))
        assertEquals(PaymentShortcut.EsunWallet, PaymentShortcut.fromId("esun_wallet"))
        assertEquals(PaymentShortcut.PlusPay, PaymentShortcut.fromId("plus_pay"))
        assertEquals(PaymentShortcut.TaishinPay, PaymentShortcut.fromId("taishin_pay"))
        assertEquals(PaymentShortcut.TaiwanPay, PaymentShortcut.fromId("taiwan_pay"))
    }

    @Test
    fun `shortcut list contains at most nine apps`() {
        assertEquals(9, PaymentShortcut.entries.size)
    }

    @Test
    fun `fromId returns null for unknown id`() {
        assertNull(PaymentShortcut.fromId(null))
        assertNull(PaymentShortcut.fromId("unknown"))
    }

    @Test
    fun `play store uris use package name`() {
        val shortcut = PaymentShortcut.IpassMoney

        assertEquals("market://details?id=com.ipass.ipassmoney", shortcut.playStoreUri)
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.ipass.ipassmoney",
            shortcut.playStoreWebUri,
        )
    }
}
