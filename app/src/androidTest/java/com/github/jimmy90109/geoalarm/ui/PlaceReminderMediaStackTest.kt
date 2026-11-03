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
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewStackCard
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewType
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderMediaStack
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderMediaLayout
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

    private fun show(
        withOverlay: Boolean = false,
        layout: PlaceReminderMediaLayout = PlaceReminderMediaLayout.Original,
    ) {
        compose.setContent {
            MaterialTheme {
                val selection = remember { mutableStateOf<MediaPreviewSelection?>(null) }
                val bounds = remember { mutableStateMapOf<String, Rect>() }
                val cards = remember { mutableStateOf<List<MediaPreviewStackCard>>(emptyList()) }
                val ratios = rememberMediaPreviewAspectRatios(shownItems.value)
                Box {
                    PlaceReminderMediaStack(shownItems.value, hidden.value, { id, rect -> bounds[id] = rect }, {
                        opened = it
                        if (withOverlay) { selection.value = it; hidden.value = it.item.id }
                    }, aspectRatiosById = ratios, onStackCardsChanged = { cards.value = it }, layout = layout)
                    selection.value?.let {
                        MediaPreviewOverlay(
                            selection = it, items = shownItems.value, sourceBoundsById = bounds.toMap(),
                            stackCards = cards.value,
                            peekAdjacentMedia = true,
                            zoomEnabled = false,
                            dismissOnBackgroundTap = true,
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

    @Test fun compactStackKeepsSizeAcrossMixedRatiosAndReturnsFromPreview() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("wide", 320, 180),
            MediaRatioFixtures.image("tall", 90, 320),
            MediaPreviewItem("video", "/missing/video.mp4", "video", MediaPreviewType.VIDEO),
        )
        show(withOverlay = true, layout = PlaceReminderMediaLayout.Compact)
        val stack = compose.onNodeWithTag("place_reminder_media_stack")
        stack.assertWidthIsEqualTo(144.dp).assertHeightIsEqualTo(144.dp)
        assertFront("wide")
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeLeft() }
        val close = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.close)
        compose.onNodeWithContentDescription(close).performClick()
        stack.assertHeightIsEqualTo(144.dp)
        front().performTouchInput { swipeLeft() }
        front().assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.place_reminder_media_description, 3, 3,
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.place_reminder_media_video),
                "video",
            ),
        ).assertHasClickAction()
        stack.assertHeightIsEqualTo(144.dp)
    }

    @Test fun compactNarrowMediaHasAccessibleHitAreaAndActualPreviewBounds() {
        val narrow = items.first().copy(width = 40, height = 800)
        compose.setContent {
            MaterialTheme {
                PlaceReminderMediaStack(
                    listOf(narrow), null, { _, _ -> }, { opened = it },
                    aspectRatiosById = mapOf(narrow.id to 0.05f),
                    layout = PlaceReminderMediaLayout.Compact,
                )
            }
        }
        front().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        front().performClick()
        compose.runOnIdle {
            assertEquals(0.05f, opened!!.sourceBounds.width / opened!!.sourceBounds.height, 0.001f)
        }
    }

    @Test fun compactVerticalDragScrollsWithoutChangingAttachment() {
        val scroll = ScrollState(0)
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    PlaceReminderMediaStack(
                        items, null, { _, _ -> }, { opened = it },
                        layout = PlaceReminderMediaLayout.Compact,
                    )
                    Spacer(Modifier.height(1000.dp))
                }
            }
        }
        front().performTouchInput { swipeUp() }
        compose.runOnIdle { assertTrue(scroll.value > 0) }
        front().performScrollTo()
        assertFront("1")
    }

    @Test fun compactSlowShortDragReboundsAndSingleAttachmentHasNoCounter() {
        show(layout = PlaceReminderMediaLayout.Compact)
        front().performTouchInput {
            swipe(center, center + Offset(width * 0.12f, 0f), durationMillis = 600)
        }
        assertFront("1")
        compose.runOnIdle { shownItems.value = items.take(1) }
        compose.onNodeWithTag("place_reminder_media_stack").assertHeightIsEqualTo(144.dp)
        compose.onNodeWithTag("place_reminder_media_count", useUnmergedTree = true).assertDoesNotExist()
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

    @Test fun promotionTimingIsIndependentOfFlingVelocity() {
        show()
        fun samplePromotion(durationMillis: Long, promotedId: String): List<Rect> {
            compose.mainClock.autoAdvance = false
            front().performTouchInput {
                swipe(center, center - Offset(width * 0.35f, 0f), durationMillis)
            }
            val card = compose.onNodeWithTag("place_reminder_media_card_$promotedId", useUnmergedTree = true)
            compose.mainClock.advanceTimeBy(64)
            val early = card.fetchSemanticsNode().boundsInRoot
            compose.mainClock.advanceTimeBy(96)
            val later = card.fetchSemanticsNode().boundsInRoot
            compose.mainClock.autoAdvance = true
            assertFront(promotedId)
            return listOf(early, later)
        }
        val slow = samplePromotion(600, "2")
        val fast = samplePromotion(60, "3")
        slow.zip(fast).forEach { (expected, actual) ->
            assertEquals(expected.left, actual.left, 1f)
            assertEquals(expected.top, actual.top, 1f)
            assertEquals(expected.right, actual.right, 1f)
            assertEquals(expected.bottom, actual.bottom, 1f)
        }
        assertTrue("Promotion must continue over time", slow.first() != slow.last())
    }

    @Test fun outgoingCardReturnsWhilePromotionIsStillRunning() {
        show(layout = PlaceReminderMediaLayout.Compact)
        compose.mainClock.autoAdvance = false
        front().performTouchInput {
            swipe(center, center + Offset(width * 1.5f, 0f), durationMillis = 60)
        }
        val outgoing = compose.onNodeWithTag("place_reminder_media_card_1", useUnmergedTree = true)
        val promoted = compose.onNodeWithTag("place_reminder_media_card_2", useUnmergedTree = true)
        compose.mainClock.advanceTimeBy(64)
        val outgoingEarly = outgoing.fetchSemanticsNode().boundsInRoot.center.x
        val promotedEarly = promoted.fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(64)
        val outgoingLater = outgoing.fetchSemanticsNode().boundsInRoot.center.x
        val promotedLater = promoted.fetchSemanticsNode().boundsInRoot
        assertTrue("Return must start before the 260 ms promotion finishes", outgoingLater < outgoingEarly - 1f)
        assertTrue("Promotion must keep running during return", promotedLater != promotedEarly)
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

    @Test fun backgroundTapDismissesSingleMediaButMediaTapDoesNot() {
        shownItems.value = listOf(MediaRatioFixtures.image("tap", 320, 180))
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performClick()
        compose.onNodeWithTag("media_preview_transition_media").performTouchInput { click(center) }
        compose.onNodeWithTag("media_preview_page_count").assertExists()
        compose.onRoot().performTouchInput { click(Offset(width / 2f, height / 4f)) }
        front().assertIsEnabled()
        compose.onNodeWithTag("media_preview_page_count").assertDoesNotExist()
    }

    @Test fun carouselBackgroundTapDismissesButMediaControlsAndSwipesDoNot() {
        shownItems.value = (1..3).map { MediaRatioFixtures.image("tap$it", 320, 180) }
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        front().performClick()
        val pager = compose.onNodeWithTag("media_preview_pager")
        pager.performTouchInput { click(center) }
        pager.assertExists()
        pager.performTouchInput { click(Offset(8f, height / 2f)) }
        pager.assertExists()
        compose.onNodeWithTag("media_preview_page_count").performTouchInput { click(center) }
        pager.assertExists()
        pager.performTouchInput {
            swipe(Offset(width * 0.75f, height / 4f), Offset(width * 0.25f, height / 4f), 400)
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithTag("media_preview_page_count").assertContentDescriptionEquals(
            context.getString(R.string.media_preview_page_description, 3, 3),
        )
        pager.performTouchInput { click(Offset(width / 2f, height / 4f)) }
        front().assertIsEnabled()
        compose.onNodeWithTag("media_preview_pager").assertDoesNotExist()
    }

    private fun assertMediaCornerIsRounded(tag: String) {
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val inset = (4f * density).toInt()
        val corner = pixels[inset, inset]
        val image = pixels[pixels.width / 4, pixels.height / 4]
        assertTrue("Media corner should stay clipped throughout expansion", corner.green < image.green * 0.65f)
    }

    @Test fun carouselRetainsRoundedCornersDuringExpansionAndAfterPagerHandoff() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("rounded", 320, 180),
            MediaRatioFixtures.image("next", 320, 180),
        )
        show(withOverlay = true)
        waitForRatio(320f / 180)
        compose.waitUntil(5000) {
            val pixels = front().captureToImage().toPixelMap()
            pixels[pixels.width / 4, pixels.height / 4].green > 0.95f
        }
        compose.mainClock.autoAdvance = false
        front().performClick()
        compose.mainClock.advanceTimeBy(128)
        assertMediaCornerIsRounded("media_preview_transition_media")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertMediaCornerIsRounded("media_preview_carousel_media_0")
    }

    @Test fun singleMediaRetainsRoundedCornersAfterExpansion() {
        shownItems.value = listOf(MediaRatioFixtures.image("rounded", 320, 180))
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performClick()
        compose.waitForIdle()
        assertMediaCornerIsRounded("media_preview_transition_media")
    }

    @Test fun verticalDragKeepsNeighborsVisibleAndStationaryUntilGroupDismisses() {
        shownItems.value = (1..3).map { MediaRatioFixtures.image("drag$it", 320, 180) }
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        front().performClick()
        compose.waitForIdle()
        val pager = compose.onNodeWithTag("media_preview_pager")
        fun bounds(page: Int) = compose.onNodeWithTag("media_preview_carousel_media_$page")
            .fetchSemanticsNode().boundsInRoot
        val left = bounds(0)
        val active = bounds(1)
        val right = bounds(2)
        compose.mainClock.autoAdvance = false
        pager.performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 0.02f), delayMillis = 32)
            moveBy(Offset(0f, height * 0.06f), delayMillis = 160)
        }
        compose.mainClock.advanceTimeBy(64)
        assertEquals(left, bounds(0))
        assertEquals(right, bounds(2))
        assertTrue(bounds(1).top > active.top)
        assertTrue(bounds(1).height < active.height)
        pager.performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(left, bounds(0))
        assertEquals(right, bounds(2))
        assertEquals(active.top, bounds(1).top, 1f)
        compose.mainClock.autoAdvance = false
        pager.performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 0.02f), delayMillis = 32)
            moveBy(Offset(0f, height * 0.24f), delayMillis = 200)
        }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(left, bounds(0))
        assertEquals(right, bounds(2))
        pager.performTouchInput { up() }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("media_preview_companion_drag1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("media_preview_companion_drag3", useUnmergedTree = true).assertExists()
        compose.mainClock.autoAdvance = true
        front().assertIsEnabled()
    }

    @Test fun carouselDisablesPinchAndDoubleTapZoomButStillPages() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("zoom", 320, 180),
            MediaRatioFixtures.image("neighbor", 320, 180),
        )
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performClick()
        compose.waitForIdle()
        val page = compose.onNodeWithTag("media_preview_carousel_page_0")
        for (vertical in listOf(false, true)) {
            page.performTouchInput {
                val start = if (vertical) Offset(0f, height * 0.06f) else Offset(width * 0.06f, 0f)
                down(0, center - start)
                down(1, center + start)
                for (step in 1..8) {
                    val fraction = 0.06f + 0.26f * step / 8f
                    val span = if (vertical) Offset(0f, height * fraction) else Offset(width * fraction, 0f)
                    moveTo(0, center - span, delayMillis = 32)
                    moveTo(1, center + span, delayMillis = 32)
                }
                up(0)
                up(1)
            }
        }
        page.performTouchInput { doubleClick() }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val pixel = pixels[pixels.width / 4, pixels.height / 3]
        assertTrue(pixel.green < 0.2f && pixel.red < 0.2f)
        page.performTouchInput { swipeLeft() }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithTag("media_preview_page_count").assertContentDescriptionEquals(
            context.getString(R.string.media_preview_page_description, 2, 2),
        )
    }

    @Test fun landscapeNeighborsKeepTheirMediaHeightAndAspectRatio() {
        shownItems.value = (1..3).map { MediaRatioFixtures.image("landscape$it", 320, 180) }
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        front().performClick()
        compose.waitForIdle()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val center = compose.onNodeWithTag("media_preview_carousel_media_1").fetchSemanticsNode().boundsInRoot
        assertEquals(320f / 180, center.width / center.height, 0.02f)
        assertTrue(center.height < root.height * 0.7f)
        for (page in listOf(0, 2)) {
            val side = compose.onNodeWithTag("media_preview_carousel_media_$page").fetchSemanticsNode().boundsInRoot
            assertTrue(side.width > 0f)
            assertEquals(center.height, side.height, 1f)
            assertEquals(center.top, side.top, 1f)
        }
    }

    @Test fun carouselShowsBothPortraitMediaEdgesWithoutWrappingAtEnds() {
        shownItems.value = (1..3).map { MediaRatioFixtures.image("edge$it", 90, 320) }
        show(withOverlay = true)
        waitForRatio(90f / 320)
        front().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        front().performClick()
        compose.waitForIdle()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val left = compose.onNodeWithTag("media_preview_carousel_page_0").fetchSemanticsNode().boundsInRoot
        val middle = compose.onNodeWithTag("media_preview_carousel_page_1").fetchSemanticsNode().boundsInRoot
        val right = compose.onNodeWithTag("media_preview_carousel_page_2").fetchSemanticsNode().boundsInRoot
        assertTrue(left.width > 0f && right.width > 0f)
        assertTrue(middle.width < root.width)
        assertEquals(root.left, left.left, 1f)
        assertEquals(root.right, right.right, 1f)
        compose.waitUntil(5000) {
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val a = pixels[8, pixels.height / 3]
            val b = pixels[pixels.width - 9, pixels.height / 3]
            a.green > 0.2f && b.green > 0.2f
        }
        val capture = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "screencap -p /sdcard/geoalarm-carousel-peeks.png",
        )
        android.os.ParcelFileDescriptor.AutoCloseInputStream(capture).use { it.readBytes() }
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeRight() }
        assertTrue(compose.onNodeWithTag("media_preview_carousel_page_0")
            .fetchSemanticsNode().boundsInRoot.left > root.left)
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeLeft() }
        assertTrue(compose.onNodeWithTag("media_preview_carousel_page_2")
            .fetchSemanticsNode().boundsInRoot.right < root.right)
    }

    @Test fun pageLabelMovesWithAnUnfinishedHorizontalSwipe() {
        show(withOverlay = true)
        front().performClick()
        compose.waitForIdle()
        val firstLabel = compose.onNodeWithTag("media_preview_page_label_1", useUnmergedTree = true)
        val initialX = firstLabel.fetchSemanticsNode().boundsInRoot.center.x
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("media_preview_pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.05f, 0f), delayMillis = 32)
            moveBy(Offset(-width * 0.2f, 0f), delayMillis = 160)
        }
        compose.mainClock.advanceTimeBy(64)
        val draggedX = firstLabel.fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("Page label should move left: $initialX -> $draggedX", draggedX < initialX)
        compose.onNodeWithTag("media_preview_pager").performTouchInput {
            moveBy(Offset(-width * 0.4f, 0f), delayMillis = 160)
            up()
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val counter = compose.onNodeWithTag("media_preview_page_count")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        counter.assertContentDescriptionEquals(context.getString(R.string.media_preview_page_description, 2, 4))
        val secondLabel = compose.onNodeWithTag("media_preview_page_label_2", useUnmergedTree = true)
        assertEquals(counter.fetchSemanticsNode().boundsInRoot.center.x,
            secondLabel.fetchSemanticsNode().boundsInRoot.center.x, 1f)
    }

    @Test fun rearCardHasDarkScrimWhileFrontStaysBright() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("front", 90, 320),
            MediaRatioFixtures.image("rear", 320, 180),
        )
        show()
        waitForRatio(90f / 320)
        val rear = compose.onNodeWithTag("place_reminder_media_card_rear", useUnmergedTree = true)
        compose.waitUntil(5000) {
            val image = rear.captureToImage().toPixelMap()
            val pixel = image[image.width / 5, image.height / 4]
            pixel.green > 0.65f && pixel.green < 0.9f && pixel.red < 0.1f
        }
        val top = front().captureToImage().toPixelMap()
        assertTrue(top[top.width / 4, top.height / 4].green > 0.95f)
    }

    @Test fun fiveAttachmentsExpandOnBothSidesAndCounterTracksOriginalOrder() {
        shownItems.value = (1..5).map { MediaRatioFixtures.image("page$it", 320, 180) }
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        front().performClick()
        compose.mainClock.advanceTimeByFrame()
        val previous = compose.onNodeWithTag("media_preview_companion_page1", useUnmergedTree = true)
        val previousStart = previous.fetchSemanticsNode().boundsInRoot.center.x
        for (id in listOf(1, 3, 4, 5)) {
            compose.onNodeWithTag("media_preview_companion_page$id", useUnmergedTree = true).assertExists()
        }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(previousStart, previous.fetchSemanticsNode().boundsInRoot.center.x, 1f)
        compose.mainClock.advanceTimeBy(128)
        assertTrue(previous.fetchSemanticsNode().boundsInRoot.center.x < previousStart)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val counter = compose.onNodeWithTag("media_preview_page_count")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun assertPage(page: Int) {
            counter.assertContentDescriptionEquals(
                context.getString(R.string.media_preview_page_description, page, 5),
            )
        }
        assertPage(2)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertEquals(root.center.x, counter.fetchSemanticsNode().boundsInRoot.center.x, 1f)
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeLeft() }
        assertPage(3)
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeRight() }
        assertPage(2)
        val capture = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "screencap -p /sdcard/geoalarm-page-count.png",
        )
        android.os.ParcelFileDescriptor.AutoCloseInputStream(capture).use { it.readBytes() }
        val close = context.getString(R.string.close)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription(close).performClick()
        compose.mainClock.advanceTimeBy(32)
        for (id in listOf(1, 3, 4, 5)) {
            compose.onNodeWithTag("media_preview_companion_page$id", useUnmergedTree = true).assertExists()
        }
        compose.mainClock.autoAdvance = true
        front().assertIsEnabled()
    }

    @Test fun singleAttachmentShowsOneOfOneCounter() {
        shownItems.value = items.take(1)
        show(withOverlay = true)
        front().performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithTag("media_preview_page_count").assertContentDescriptionEquals(
            context.getString(R.string.media_preview_page_description, 1, 1),
        )
        compose.onNodeWithTag("media_preview_pager").assertDoesNotExist()
    }

    @Test fun promotedCardStaysUprightWhileOutgoingCardReturns() {
        show()
        compose.mainClock.autoAdvance = false
        front().performTouchInput { swipeLeft(durationMillis = 100) }
        var checkedPromotion = false
        repeat(70) {
            compose.mainClock.advanceTimeByFrame()
            val top = front().fetchSemanticsNode()
            val description = top.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription]
            if (description.any { it.contains("photo2") }) {
                checkedPromotion = true
                val card = compose.onNodeWithTag("place_reminder_media_card_2", useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                assertEquals(top.boundsInRoot.width, card.width, 1f)
                assertEquals(top.boundsInRoot.height, card.height, 1f)
            }
        }
        assertTrue(checkedPromotion)
        compose.mainClock.autoAdvance = true
        assertFront("2")
    }

    @Test fun cardCompositionSurvivesOutgoingAndReturnForMixedImages() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("wide", 320, 180),
            MediaRatioFixtures.image("tall", 180, 320),
            MediaRatioFixtures.image("square", 240, 240),
            MediaRatioFixtures.image("next", 280, 180),
        )
        show()
        waitForRatio(320f / 180)
        val card = compose.onNodeWithTag("place_reminder_media_card_wide", useUnmergedTree = true)
        val identity = card.fetchSemanticsNode().id
        compose.mainClock.autoAdvance = false
        front().performTouchInput { swipeLeft(durationMillis = 100) }
        repeat(50) {
            compose.mainClock.advanceTimeBy(16)
            assertEquals(identity, card.fetchSemanticsNode().id)
        }
        compose.mainClock.autoAdvance = true
        assertFront("tall")
        assertEquals(identity, card.fetchSemanticsNode().id)
    }

    @Test fun overlayCompanionsExpandInSequenceAndReturnTogether() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("first", 320, 180),
            MediaRatioFixtures.image("second", 320, 180),
            MediaRatioFixtures.image("third", 320, 180),
        )
        show(withOverlay = true)
        waitForRatio(320f / 180)
        compose.mainClock.autoAdvance = false
        front().performClick()
        compose.mainClock.advanceTimeByFrame()
        val second = compose.onNodeWithTag("media_preview_companion_second", useUnmergedTree = true)
        val third = compose.onNodeWithTag("media_preview_companion_third", useUnmergedTree = true)
        val start = second.fetchSemanticsNode().boundsInRoot
        val thirdStart = third.fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(64)
        val moving = second.fetchSemanticsNode().boundsInRoot
        assertTrue(moving.center.x > start.center.x)
        assertEquals(thirdStart, third.fetchSemanticsNode().boundsInRoot)
        compose.mainClock.advanceTimeBy(48)
        assertTrue(third.fetchSemanticsNode().boundsInRoot.center.x > thirdStart.center.x)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        second.assertDoesNotExist()
        compose.onNodeWithTag("media_preview_pager").performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals("second", hidden.value) }
        compose.mainClock.autoAdvance = false
        val close = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.close)
        compose.onNodeWithContentDescription(close).performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("media_preview_companion_third", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("media_preview_companion_first", useUnmergedTree = true).assertExists()
        compose.mainClock.autoAdvance = true
        assertFront("second")
        compose.onNodeWithTag("media_preview_companion_third", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun middleAttachmentExpandsInOriginalOrderAndCancelledDismissKeepsOverlay() {
        shownItems.value = listOf(
            MediaRatioFixtures.image("first", 320, 180),
            MediaRatioFixtures.image("middle", 320, 180),
            MediaRatioFixtures.image("last", 320, 180),
        )
        show(withOverlay = true)
        waitForRatio(320f / 180)
        front().performTouchInput { swipeLeft() }
        assertFront("middle")
        // assertFront opens the overlay through the same callback as a user tap.
        compose.waitForIdle()
        val close = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.close)
        compose.onNodeWithContentDescription(close).performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        front().performClick()
        compose.mainClock.advanceTimeByFrame()
        val left = compose.onNodeWithTag("media_preview_companion_first", useUnmergedTree = true)
        val right = compose.onNodeWithTag("media_preview_companion_last", useUnmergedTree = true)
        val leftStart = left.fetchSemanticsNode().boundsInRoot.center.x
        val rightStart = right.fetchSemanticsNode().boundsInRoot.center.x
        compose.mainClock.advanceTimeBy(160)
        assertTrue(left.fetchSemanticsNode().boundsInRoot.center.x < leftStart)
        assertTrue(right.fetchSemanticsNode().boundsInRoot.center.x > rightStart)
        val capture = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "screencap -p /sdcard/geoalarm-group-expansion.png",
        )
        android.os.ParcelFileDescriptor.AutoCloseInputStream(capture).use { it.readBytes() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("media_preview_pager").performTouchInput {
            swipe(center, center + Offset(0f, height * 0.05f), durationMillis = 600)
        }
        compose.onNodeWithTag("media_preview_pager").assertExists()
        androidx.test.espresso.Espresso.pressBack()
        front().assertIsEnabled()
        compose.runOnIdle { assertEquals("middle", opened?.item?.id) }
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
