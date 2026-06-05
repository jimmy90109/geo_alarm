package com.github.jimmy90109.geoalarm.data

enum class PaymentShortcut(
    val id: String,
    val displayName: String,
    val packageName: String,
) {
    EasyWallet(
        id = "easy_wallet",
        displayName = "悠遊付",
        packageName = "com.easycard.wallet",
    ),
    JkoPay(
        id = "jkopay",
        displayName = "街口支付",
        packageName = "com.jkos.app",
    ),
    IpassMoney(
        id = "ipass_money",
        displayName = "iPASS MONEY",
        packageName = "com.ipass.ipassmoney",
    ),
    IcashPay(
        id = "icash_pay",
        displayName = "icash Pay",
        packageName = "tw.com.icash.a.icashpay",
    ),
    PxPayPlus(
        id = "pxpay_plus",
        displayName = "全支付",
        packageName = "com.pxpayplus.app",
    ),
    EsunWallet(
        id = "esun_wallet",
        displayName = "玉山 Wallet",
        packageName = "com.esunbank.ESUNWALLET",
    ),
    PlusPay(
        id = "plus_pay",
        displayName = "全盈+PAY",
        packageName = "tw.com.pluspay.oneapp",
    ),
    TaishinPay(
        id = "taishin_pay",
        displayName = "台新 Pay",
        packageName = "tw.com.taishinbank.ccapp",
    ),
    TaiwanPay(
        id = "taiwan_pay",
        displayName = "台灣 Pay",
        packageName = "tw.com.twmp.twhcewallet",
    );

    val playStoreUri: String
        get() = "market://details?id=$packageName"

    val playStoreWebUri: String
        get() = "https://play.google.com/store/apps/details?id=$packageName"

    companion object {
        fun fromId(id: String?): PaymentShortcut? = entries.firstOrNull { it.id == id }
    }
}
