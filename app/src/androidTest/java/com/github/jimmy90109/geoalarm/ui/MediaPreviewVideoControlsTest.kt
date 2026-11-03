package com.github.jimmy90109.geoalarm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewVideoControls
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaPreviewVideoControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pauseToggleAndSeekUpdateControls() {
        val playing = mutableStateOf(true)
        val position = mutableStateOf(10_000L)
        var seekPosition: Long? = null
        compose.setContent {
            MaterialTheme {
                MediaPreviewVideoControls(
                    positionMillis = position.value,
                    durationMillis = 60_000L,
                    playing = playing.value,
                    canSeek = true,
                    onTogglePlayback = { playing.value = !playing.value },
                    onSeek = { seekPosition = it; position.value = it },
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithContentDescription(context.getString(R.string.media_preview_pause)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.media_preview_play)).assertExists()
        compose.onNodeWithTag("media_preview_seek").performSemanticsAction(SemanticsActions.SetProgress) {
            it(0.75f)
        }
        compose.runOnIdle {
            assertEquals(45_000L, seekPosition)
            assertEquals(false, playing.value)
        }
        compose.onNodeWithText("0:45 / 1:00").assertExists()
    }

    @Test fun unavailableDurationDisablesSeeking() {
        compose.setContent {
            MaterialTheme {
                MediaPreviewVideoControls(
                    positionMillis = 0L,
                    durationMillis = 0L,
                    playing = false,
                    canSeek = false,
                    onTogglePlayback = {},
                    onSeek = { error("Unavailable media must not seek") },
                )
            }
        }
        compose.onNodeWithTag("media_preview_seek").assertIsNotEnabled()
    }
}
