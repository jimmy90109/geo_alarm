package com.github.jimmy90109.geoalarm.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.jimmy90109.geoalarm.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun HomeNativeAdCard(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
) {
    val compact = LocalConfiguration.current.screenWidthDp >= 600
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow.toArgb()
    val contentColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val label = stringResource(R.string.ad_label)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 136.dp else 172.dp)
                .padding(16.dp),
            factory = { context ->
                val nativeAdView = NativeAdView(context)
                val content = LinearLayout(context).apply {
                    setPadding(0, 0, 0, 0)
                }
                val adLabel = TextView(context).apply {
                    textSize = 12f
                    includeFontPadding = false
                    setTextColor(primaryColor)
                }
                val mediaView = MediaView(context).apply {
                    setBackgroundColor(containerColor)
                }
                val headline = TextView(context).apply {
                    textSize = 16f
                    maxLines = 2
                    includeFontPadding = false
                    setTextColor(contentColor)
                }
                val body = TextView(context).apply {
                    textSize = 13f
                    maxLines = 2
                    includeFontPadding = false
                    setTextColor(secondaryColor)
                }
                val advertiser = TextView(context).apply {
                    textSize = 12f
                    maxLines = 1
                    includeFontPadding = false
                    setTextColor(secondaryColor)
                }
                val icon = ImageView(context).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                val callToAction = Button(context).apply {
                    textSize = 13f
                    minHeight = 0
                    minWidth = 0
                    minimumHeight = 0
                    minimumWidth = 0
                    includeFontPadding = false
                    setPadding(
                        12.dpToPx(context),
                        6.dpToPx(context),
                        12.dpToPx(context),
                        6.dpToPx(context),
                    )
                }

                nativeAdView.addView(content)
                nativeAdView.setBackgroundColor(containerColor)

                nativeAdView.headlineView = headline
                nativeAdView.bodyView = body
                nativeAdView.advertiserView = advertiser
                nativeAdView.iconView = icon
                nativeAdView.callToActionView = callToAction
                nativeAdView.mediaView = mediaView
                nativeAdView.tag = NativeAdViewHolder(
                    label = adLabel,
                    headline = headline,
                    body = body,
                    advertiser = advertiser,
                    icon = icon,
                    callToAction = callToAction,
                    mediaView = mediaView,
                    content = content,
                )
                nativeAdView
            },
            update = { nativeAdView ->
                val holder = nativeAdView.tag as NativeAdViewHolder
                holder.content.removeAllViews()
                holder.detachAssetViews()
                holder.content.addView(
                    buildNativeAdLayout(
                        holder = holder,
                        compact = compact,
                    )
                )
                holder.label.text = label
                holder.headline.text = nativeAd.headline
                holder.body.setOptionalText(nativeAd.body)
                holder.advertiser.setOptionalText(nativeAd.advertiser)
                holder.callToAction.setOptionalText(nativeAd.callToAction)
                holder.icon.setImageDrawable(nativeAd.icon?.drawable)
                holder.icon.visibility = if (nativeAd.icon?.drawable != null) View.VISIBLE else View.GONE
                holder.mediaView.mediaContent = nativeAd.mediaContent
                holder.mediaView.visibility = if (nativeAd.mediaContent != null) View.VISIBLE else View.GONE
                nativeAdView.setNativeAd(nativeAd)
            },
        )
    }
}

private data class NativeAdViewHolder(
    val content: LinearLayout,
    val label: TextView,
    val headline: TextView,
    val body: TextView,
    val advertiser: TextView,
    val icon: ImageView,
    val callToAction: Button,
    val mediaView: MediaView,
)

private fun NativeAdViewHolder.detachAssetViews() {
    listOf(label, headline, body, advertiser, icon, callToAction, mediaView).forEach { view ->
        (view.parent as? ViewGroup)?.removeView(view)
    }
}

private fun buildNativeAdLayout(
    holder: NativeAdViewHolder,
    compact: Boolean,
): View {
    val context = holder.content.context
    return if (compact) {
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    addView(
                        holder.mediaView,
                        LinearLayout.LayoutParams(
                    120.dpToPx(context),
                    120.dpToPx(context),
                ).apply {
                    marginEnd = 14.dpToPx(context)
                },
            )
            addView(
                buildTextColumn(holder, compact = true),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
    } else {
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                holder.mediaView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    120.dpToPx(context),
                ).apply {
                    bottomMargin = 10.dpToPx(context)
                },
            )
            addView(buildTextColumn(holder, compact = false))
        }
    }
}

private fun buildTextColumn(
    holder: NativeAdViewHolder,
    compact: Boolean,
): View {
    val context = holder.content.context
    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(holder.label)
        addView(
            holder.headline,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 4.dpToPx(context)
            },
        )
        addView(
            holder.body,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 4.dpToPx(context)
            },
        )
        addView(
            buildFooterRow(holder, compact),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = if (compact) 8.dpToPx(context) else 10.dpToPx(context)
            },
        )
    }
}

private fun buildFooterRow(
    holder: NativeAdViewHolder,
    compact: Boolean,
): View {
    val context = holder.content.context
    return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        addView(
            holder.icon,
            LinearLayout.LayoutParams(
                32.dpToPx(context),
                32.dpToPx(context),
            ).apply {
                marginEnd = 8.dpToPx(context)
            },
        )
        addView(
            holder.advertiser,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8.dpToPx(context)
            },
        )
        addView(
            holder.callToAction,
            LinearLayout.LayoutParams(
                if (compact) LinearLayout.LayoutParams.WRAP_CONTENT else 120.dpToPx(context),
                38.dpToPx(context),
            ),
        )
    }
}

private fun TextView.setOptionalText(value: String?) {
    text = value.orEmpty()
    visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
}

private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
