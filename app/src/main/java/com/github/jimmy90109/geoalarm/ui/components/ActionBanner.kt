package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ActionBanner(
    icon: ImageVector,
    message: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(300),
        label = "bannerContainerColor",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(300),
        label = "bannerContentColor",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = animatedContainerColor,
        contentColor = animatedContentColor,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                )
            }
            AnimatedVisibility(
                visible = showActions,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = FastOutSlowInEasing,
                    ),
                    expandFrom = Alignment.Top,
                ),
                exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = FastOutSlowInEasing,
                    ),
                    shrinkTowards = Alignment.Top,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
fun ActionBannerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.62f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
