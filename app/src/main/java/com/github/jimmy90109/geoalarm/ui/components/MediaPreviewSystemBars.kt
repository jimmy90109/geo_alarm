package com.github.jimmy90109.geoalarm.ui.components

import android.os.CancellationSignal
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsAnimationController
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

@Composable
internal fun MediaPreviewSystemBars(progress: () -> Float) {
    val view = LocalView.current
    val initialInsets = remember(view) { view.rootWindowInsets }
    val latestProgress by rememberUpdatedState(progress)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    LaunchedEffect(view, lifecycleState) {
        if (view.isInEditMode || lifecycleState != Lifecycle.State.RESUMED) return@LaunchedEffect
        val session = MediaPreviewSystemBarsSession(view, initialInsets)
        try {
            snapshotFlow { latestProgress().coerceIn(0f, 1f) }.collect(session::update)
        } finally {
            session.restore()
        }
    }
}

private class MediaPreviewSystemBarsSession(view: View, originalInsets: WindowInsets?) {
    private val controller = view.windowInsetsController
    private val originalBehavior = controller?.systemBarsBehavior
    private val types = listOf(WindowInsets.Type.statusBars(), WindowInsets.Type.navigationBars())
    private val visibleTypes = types.filter { originalInsets?.isVisible(it) == true }.fold(0, Int::or)
    private val hiddenTypes = types.filter { originalInsets?.isVisible(it) == false }.fold(0, Int::or)
    private var animation: WindowInsetsAnimationController? = null
    private var request: CancellationSignal? = null
    private var progress = 0f
    private var settledHidden = false
    private var useSystemAnimation = false
    private var disposed = false

    init {
        controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun update(value: Float) {
        progress = value
        if (disposed || visibleTypes == 0 || controller == null) return
        animation?.let {
            applyProgress(it)
            return
        }
        if (request != null || (progress == 0f && !settledHidden) || (progress == 1f && settledHidden)) return
        if (useSystemAnimation) {
            applySystemVisibility()
            return
        }
        val cancellation = CancellationSignal()
        request = cancellation
        controller.controlWindowInsetsAnimation(
            visibleTypes,
            -1L,
            null,
            cancellation,
            object : WindowInsetsAnimationControlListener {
                override fun onReady(controller: WindowInsetsAnimationController, types: Int) {
                    if (disposed) {
                        controller.finish(true)
                        return
                    }
                    animation = controller
                    applyProgress(controller)
                }

                override fun onFinished(controller: WindowInsetsAnimationController) {
                    animation = null
                    request = null
                }

                override fun onCancelled(controller: WindowInsetsAnimationController?) {
                    animation = null
                    request = null
                    useSystemAnimation = true
                    if (!disposed) applySystemVisibility()
                }
            },
        )
    }

    private fun applyProgress(controller: WindowInsetsAnimationController) {
        if (!controller.isReady) return
        // Fade in place; stable layout insets also survive finish(false) at the hidden endpoint.
        controller.setInsetsAndAlpha(controller.shownStateInsets, 1f - progress, 1f - progress)
        if (progress == 0f || progress == 1f) {
            settledHidden = progress == 1f
            controller.finish(!settledHidden)
        }
    }

    private fun applySystemVisibility() {
        val hide = progress >= 0.5f
        if (hide == settledHidden) return
        settledHidden = hide
        if (hide) controller?.hide(visibleTypes) else controller?.show(visibleTypes)
    }

    fun restore() {
        disposed = true
        request?.cancel()
        if (visibleTypes != 0) controller?.show(visibleTypes)
        if (hiddenTypes != 0) controller?.hide(hiddenTypes)
        originalBehavior?.let { controller?.systemBarsBehavior = it }
    }
}
