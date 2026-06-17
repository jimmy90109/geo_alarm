package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    showAlternateTitle: Boolean = false,
    alternateTitle: (@Composable () -> Unit)? = null
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = maxOf(statusBar, 24.dp)

    Surface(
        modifier = modifier.padding(top = topPadding),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 4.dp
    ) {
        CenterAlignedTopAppBar(
            title = {
                AnimatedContent(
                    targetState = showAlternateTitle && alternateTitle != null,
                    transitionSpec = {
                        val transition = if (targetState) {
                            (slideInHorizontally(tween(280)) { it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(160)))
                        } else {
                            (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(160)))
                        }
                        transition.using(SizeTransform(clip = true) { _, _ -> snap() })
                    },
                    contentAlignment = Alignment.Center,
                    label = "TopAppBarTitleTransition"
                ) { showAlternate ->
                    if (showAlternate) alternateTitle?.invoke() else title()
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
    }
}
