package com.github.jimmy90109.geoalarm.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object PlayStoreListingLauncher {
    private const val RELEASE_PACKAGE_NAME = "com.github.jimmy90109.geoalarm"

    fun open(context: Context) {
        val marketIntent = createIntent("market://details?id=$RELEASE_PACKAGE_NAME")
        val target = when (
            planTarget(marketIntent.resolveActivity(context.packageManager) != null)
        ) {
            ListingTarget.Market -> marketIntent
            ListingTarget.Web -> null
        }

        runCatching {
            if (target != null) {
                context.startActivity(target)
            } else {
                WebPageLauncher.open(context, webUrl())
            }
        }.recoverCatching {
            WebPageLauncher.open(context, webUrl())
        }
    }

    internal fun webUrl(): String =
        "https://play.google.com/store/apps/details?id=$RELEASE_PACKAGE_NAME"

    private fun createIntent(uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    internal fun planTarget(canHandleMarketIntent: Boolean): ListingTarget =
        if (canHandleMarketIntent) ListingTarget.Market else ListingTarget.Web

    internal enum class ListingTarget {
        Market,
        Web,
    }
}
