package com.github.jimmy90109.geoalarm.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.rememberMediaPreviewAspectRatios
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewOverlay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertTrue
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewType
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderMediaStack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaceReminderMediaStackTest {
    private val motionScale = object : MotionDurationScale { override var scaleFactor = 1f }
    @get:Rule val compose = createComposeRule(effectContext = motionScale)
    private val items = (1..4).map {
        MediaPreviewItem("$it", "/missing/photo$it.jpg", "photo$it", MediaPreviewType.IMAGE)
    }
    private var opened: MediaPreviewSelection? = null
    private val hidden = mutableStateOf<String?>(null)
    private val shownItems = mutableStateOf(items)

    private fun show(withOverlay: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                val selection = remember { mutableStateOf<MediaPreviewSelection?>(null) }
                val bounds = remember { mutableStateMapOf<String, Rect>() }
                val ratios = rememberMediaPreviewAspectRatios(shownItems.value)
                Box {
                    PlaceReminderMediaStack(shownItems.value, hidden.value, { id, rect -> bounds[id] = rect }, {
                        opened = it
                        if (withOverlay) { selection.value = it; hidden.value = it.item.id }
                    }, aspectRatiosById = ratios)
                    selection.value?.let {
                        MediaPreviewOverlay(
                            selection = it, items = shownItems.value, sourceBoundsById = bounds.toMap(),
                            sourceCornerRadius = 16.dp, aspectRatiosById = ratios,
                            onActiveItemChanged = { active -> hidden.value = active.id },
                            onDismiss = { selection.value = null; hidden.value = null },
                        )
                    }
                }
            }
        }
    }
    private fun front() = compose.onNodeWithTag("place_reminder_media_front")
    private fun assertFront(id: String) {
        front().performClick()
        compose.runOnIdle { assertEquals(id, opened?.item?.id) }
    }

    @Test fun bothDirectionsCycleToBottomAndOpenCurrentItem() {
        show()
        assertFront("1")
        front().performTouchInput { swipeLeft() }
        assertFront("2")
        front().performTouchInput { swipeRight() }
        assertFront("3")
        front().performTouchInput { swipeLeft() }
        assertFront("4")
        front().performTouchInput { swipeRight() }
        assertFront("1")
    }

    @Test fun shortSlowDragReturnsToSameCard() {
        show()
        front().performTouchInput {
            swipe(center, center + Offset(width * 0.12f, 0f), durationMillis = 600)
        }
        assertFront("1")
    }

    @Test fun overlaySelectionAndRemovedAttachmentReconcileFrontCard() {
        show()
        compose.runOnIdle { hidden.value = "3" }
        compose.waitForIdle()
        compose.runOnIdle { hidden.value = null }
        assertFront("3")
        compose.runOnIdle { shownItems.value = items.filterNot { it.id == "3" } }
        assertFront("1")
    }

    @Test fun animationBlocksRepeatedSwipesAndClicks() {
        show()
        compose.mainClock.autoAdvance = false
        front().performTouchInput { swipeLeft(durationMillis = 100) }
        compose.mainClock.advanceTimeByFrame()
        front().assertIsNotEnabled()
        front().performTouchInput { swipeRight(durationMillis = 100) }
        front().performClick()
        compose.runOnIdle { assertEquals(null, opened) }
        compose.mainClock.autoAdvance = true
        assertFront("2")
    }

    @Test fun singleItemDoesNotCycleAndEmptyListHidesMedia() {
        shownItems.value = items.take(1)
        show()
        front().performTouchInput { swipeLeft() }
        assertFront("1")
        compose.runOnIdle { shownItems.value = emptyList() }
        front().assertDoesNotExist()
    }
    @Test fun fullScreenUsesOriginalOrderAndReturnsToLastViewedCard() {
        show(withOverlay = true)
        front().performTouchInput { swipeLeft() }
        front().performTouchInput { swipeLeft() }
        assertFront("3")
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeRight() }
        compose.runOnIdle { assertEquals("2", hidden.value) }
        val close = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.close)
        compose.onNodeWithContentDescription(close).performClick()
        assertFront("2")
    }

    @Test fun attachmentUpdateCancelsInFlightAnimation() {
        show()
        compose.mainClock.autoAdvance = false
        front().performTouchInput { swipeLeft(durationMillis = 100) }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { shownItems.value = items.filterNot { it.id == "2" } }
        compose.mainClock.autoAdvance = true
        assertFront("1")
    }

    @Test fun savedFrontCardSurvivesRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                PlaceReminderMediaStack(items, null, { _, _ -> }, { opened = it })
            }
        }
        front().performTouchInput { swipeLeft() }
        assertFront("2")
        restoration.emulateSavedInstanceStateRestore()
        assertFront("2")
    }

    @Test fun sixMixedAttachmentsIncludingMissingVideoRemainBrowsable() {
        shownItems.value = (1..6).map {
            MediaPreviewItem("$it", "/missing/media$it", "media$it",
                if (it % 2 == 0) MediaPreviewType.VIDEO else MediaPreviewType.IMAGE,
                durationMillis = if (it % 2 == 0) 19_000L else null)
        }
        show()
        for (id in 1..6) {
            assertFront("$id")
            front().performTouchInput { swipeLeft() }
        }
        assertFront("1")
    }

    @Test fun twoCardsCycleAndAccessibilityActionAdvances() {
        shownItems.value = items.take(2)
        show()
        val actions = front().fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            assertEquals(1, actions.size)
            assertTrue(actions.single().action())
        }
        assertFront("2")
        front().performTouchInput { swipeRight() }
        assertFront("1")
    }

    @Test fun verticalSwipeScrollsPageWithoutCycling() {
        val scroll = ScrollState(0)
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    PlaceReminderMediaStack(items, null, { _, _ -> }, { opened = it })
                    Spacer(Modifier.height(1000.dp))
                }
            }
        }
        front().performTouchInput { swipeUp() }
        compose.runOnIdle { assertTrue(scroll.value > 0) }
        front().performScrollTo()
        assertFront("1")
    }

    @Test fun disabledAnimationsAdvanceWithoutBlockingInteractions() {
        motionScale.scaleFactor = 0f
        show()
        compose.mainClock.autoAdvance = false
        front().performTouchInput { swipeLeft(durationMillis = 100) }
        compose.mainClock.advanceTimeByFrame()
        front().assertIsEnabled()
        assertFront("2")
        compose.mainClock.autoAdvance = true
    }

    private fun waitForRatio(ratio: Float) {
        compose.waitUntil(5000) {
            val bounds = front().fetchSemanticsNode().boundsInRoot
            kotlin.math.abs(bounds.width / bounds.height - ratio) < 0.02f
        }
    }

    @Test fun mixedActualMediaKeepTheirRatiosAndRegionHeightThroughFullCycle() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("wide", 320, 180),
            MediaRatioFixtures.image("tall", 180, 320),
            MediaRatioFixtures.image("square", 240, 240),
            MediaRatioFixtures.image("long", 40, 800),
            MediaRatioFixtures.image("exif", 320, 180, true),
            MediaRatioFixtures.video(true),
        )
        val ratios = listOf(320f / 180, 180f / 320, 1f, 0.05f, 180f / 320, 180f / 320)
        show()
        waitForRatio(ratios.first())
        val region = compose.onNodeWithTag("place_reminder_media_region")
        val height = region.fetchSemanticsNode().boundsInRoot.height
        for (ratio in ratios) {
            waitForRatio(ratio)
            assertEquals(height, region.fetchSemanticsNode().boundsInRoot.height, 1f)
            front().performTouchInput { swipe(center, center - Offset(width.toFloat(), 0f)) }
        }
        waitForRatio(ratios.first())
        assertFront("wide")
    }

    @Test fun narrowPortraitUsesOwnWidthForThresholdAndReturnsFromOverlayToCorrectBounds() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("wide", 320, 180),
            MediaRatioFixtures.image("tall", 180, 320),
        )
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performTouchInput { swipeLeft() }
        waitForRatio(180f / 320)
        val bounds = front().fetchSemanticsNode().boundsInRoot
        front().performClick()
        compose.runOnIdle {
            assertEquals("tall", opened?.item?.id)
            assertEquals(bounds.width, opened!!.sourceBounds.width, 1f)
            assertEquals(bounds.height, opened!!.sourceBounds.height, 1f)
        }
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeRight() }
        val close = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.close)
        compose.onNodeWithContentDescription(close).performClick()
        waitForRatio(320f / 180)
        assertFront("wide")
    }

    @Test fun slowQuarterWidthPortraitDragAdvancesWhileShortDragReturns() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("portrait", 90, 320),
            MediaRatioFixtures.image("next", 320, 180),
        )
        show()
        waitForRatio(90f / 320)
        front().performTouchInput { swipe(center, center + Offset(width * 0.12f, 0f), 600) }
        assertFront("portrait")
        front().performTouchInput { swipe(center, center + Offset(width * 0.30f, 0f), 600) }
        waitForRatio(320f / 180)
        assertFront("next")
    }

    @Test fun portraitAndLandscapeWindowSizesCapHeightWithoutChangingAspectRatio() {
        val item = MediaRatioFixtures.image("window", 180, 320)
        val size = mutableStateOf(IntSize(900, 1800))
        val window = object : WindowInfo {
            override val isWindowFocused = true
            override val containerSize get() = size.value
        }
        compose.setContent {
            CompositionLocalProvider(LocalWindowInfo provides window) {
                MaterialTheme { PlaceReminderMediaStack(listOf(item), null, { _, _ -> }, { opened = it }) }
            }
        }
        waitForRatio(180f / 320)
        compose.waitUntil(5000) {
            val image = front().captureToImage().toPixelMap()
            val pixel = image[image.width / 4, image.height / 4]
            pixel.green > 0.7f && pixel.red < 0.3f
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun save(name: String) {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(context.externalCacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val portraitHeight = front().fetchSemanticsNode().boundsInRoot.height
        save("ratio-stack-portrait.png")
        compose.runOnIdle { size.value = IntSize(1800, 900) }
        waitForRatio(180f / 320)
        assertTrue(front().fetchSemanticsNode().boundsInRoot.height < portraitHeight)
        save("ratio-stack-landscape.png")
    }

}
