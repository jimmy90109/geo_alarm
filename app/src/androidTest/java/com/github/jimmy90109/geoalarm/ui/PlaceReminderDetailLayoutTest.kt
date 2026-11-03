package com.github.jimmy90109.geoalarm.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.*
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.PlaceReminderDetailContent
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.PlaceReminderDetailActionBar
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlaceReminderDetailLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(reminder())
    private var resetCount = 0
    private val viewportHeight = mutableStateOf(800)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun reminder(
        text: String = "記得帶環保袋",
        checklist: Boolean = false,
        attachments: Boolean = true,
        itemCount: Int = 0,
    ): PlaceReminderWithItems {
        val reminder = PlaceReminder(
            id = "detail", title = "採買提醒", type = if (checklist) PlaceReminderType.CHECKLIST else PlaceReminderType.TEXT,
            content = text, placeName = "超市", address = null, latitude = 25.0, longitude = 121.0,
            radiusMeters = 200, triggerType = PlaceTriggerType.ENTER, dwellMinutes = null,
            cooldownMinutes = 60, enabled = false, lastTriggeredAt = null, createdAt = 0, updatedAt = 0,
        )
        return PlaceReminderWithItems(
            reminder,
            (1..itemCount).map { PlaceReminderItem("item$it", reminder.id, "待辦項目 $it", false, it) },
            if (attachments) listOf(PlaceReminderAttachment(
                id = "photo", reminderId = reminder.id, type = PlaceReminderAttachmentType.IMAGE,
                localPath = "/missing/photo.jpg", mimeType = "image/jpeg", displayName = "照片",
                sizeBytes = 0, durationMillis = null, width = 320, height = 180, sortOrder = 0, createdAt = 0,
            )) else emptyList(),
        )
    }

    private fun show(
        fontScale: Float = 1f,
        landscape: Boolean = false,
        height: Int = 800,
        withActionBar: Boolean = false,
    ) {
        viewportHeight.value = height
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(
                if (landscape) DpSize(800.dp, 360.dp) else DpSize(360.dp, viewportHeight.value.dp),
            )) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    GeoAlarmTheme {
                        Surface {
                            val density = LocalDensity.current
                            var actionBarHeight by remember { mutableStateOf(0) }
                            Box(Modifier.fillMaxSize()) {
                                PlaceReminderDetailContent(
                                    reminderWithItems = state.value, newItemText = "", onNewItemTextChange = {},
                                    onResetChecklist = { resetCount++ }, onItemCheckedChange = { _, _ -> }, onAddItem = {},
                                    attachmentAspectRatios = mapOf("photo" to (320f / 180)),
                                    onStackCardsChanged = {}, hiddenAttachmentId = null,
                                    onAttachmentBoundsChanged = { _, _ -> }, onAttachmentClick = {},
                                    contentPadding = if (landscape) PaddingValues() else PaddingValues(
                                        top = 96.dp,
                                        bottom = if (withActionBar) with(density) { actionBarHeight.toDp() } else 144.dp,
                                    ),
                                    isLandscape = landscape, modifier = Modifier.fillMaxSize(),
                                )
                                if (withActionBar) {
                                    PlaceReminderDetailActionBar(
                                        lastTriggeredAt = null, onEdit = {}, onDelete = {},
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                            .onSizeChanged { actionBarHeight = it.height }
                                            .testTag("detail_action_bar"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scrollRange() = compose.onNodeWithTag("place_reminder_detail_scroll")
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(context.externalCacheDir, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Keep layout captures available after Gradle removes the test app.
        val copy = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "cp ${file.absolutePath} /data/local/tmp/$name",
        )
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
    }

    @Test fun shortTextUsesCompactMediaWithoutOverlapOrScrolling() {
        show()
        val stack = compose.onNodeWithTag("place_reminder_media_stack")
        stack.assertWidthIsEqualTo(144.dp).assertHeightIsEqualTo(144.dp)
        val media = stack.fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(context.getString(R.string.place_reminder_content))
            .fetchSemanticsNode().boundsInRoot
        assertEquals(with(compose.density) { 32.dp.toPx() }, media.bottom - title.top, 1f)
        assertTrue(title.right <= media.left)
        val summary = compose.onNodeWithTag("place_reminder_summary").fetchSemanticsNode().boundsInRoot
        val place = compose.onNodeWithText("超市").fetchSemanticsNode().boundsInRoot
        assertTrue(place.center.x < summary.center.x)
        assertEquals(with(compose.density) { 8.dp.toPx() }, media.top - summary.bottom, 1f)
        compose.onNodeWithText("記得帶環保袋").assertIsDisplayed()
        assertEquals(0f, scrollRange().maxValue(), 1f)
        save("detail-compact-text.png")
    }

    @Test fun compactSwipeDrawsBeyondAttachmentContainer() {
        val file = File(context.cacheDir, "swipe-red.png")
        val red = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        file.outputStream().use { red.compress(Bitmap.CompressFormat.PNG, 100, it) }
        red.recycle()
        val initial = reminder()
        val attachment = initial.attachments.single().copy(localPath = file.path)
        state.value = initial.copy(attachments = listOf(attachment, attachment.copy(id = "photo2", sortOrder = 1)))
        show()
        val front = compose.onNodeWithTag("place_reminder_media_front")
        val bounds = front.fetchSemanticsNode().boundsInRoot
        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        fun isRedAt(x: Float, y: Float): Boolean {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val pixel = bitmap.getPixel((x - rootBounds.left).toInt(), (y - rootBounds.top).toInt())
            return Color.red(pixel) > 220 && Color.green(pixel) < 50 && Color.blue(pixel) < 50
        }
        compose.waitUntil(5_000) { isRedAt(bounds.center.x, bounds.center.y) }
        front.performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.75f, 0f), delayMillis = 200)
        }
        val stack = compose.onNodeWithTag("place_reminder_media_stack").fetchSemanticsNode().boundsInRoot
        assertTrue("Dragged card must remain visible outside its attachment container",
            isRedAt(stack.left - with(compose.density) { 24.dp.toPx() }, bounds.center.y))
        save("detail-compact-swipe-overflow.png")
        front.performTouchInput { up() }
    }

    @Test fun attachmentOnlyFillsRemainingSpaceAndNoAttachmentLeavesNoGap() {
        state.value = reminder(text = "")
        show()
        val stack = compose.onNodeWithTag("place_reminder_media_stack")
        stack.assertWidthIsEqualTo(312.dp)
        assertTrue(stack.fetchSemanticsNode().boundsInRoot.height > with(compose.density) { 144.dp.toPx() })
        assertEquals(0f, scrollRange().maxValue(), 1f)
        save("detail-attachment-only.png")
        compose.runOnIdle { state.value = reminder(attachments = false) }
        stack.assertDoesNotExist()
        compose.onNodeWithText("記得帶環保袋").assertIsDisplayed()
        assertEquals(0f, scrollRange().maxValue(), 1f)
    }

    @Test fun emptyChecklistStillUsesCompactMediaAndTextChecklistHasOnlyOneStack() {
        state.value = reminder(text = "", checklist = true)
        show()
        compose.onNodeWithTag("place_reminder_media_stack").assertHeightIsEqualTo(144.dp)
        val title = compose.onNodeWithText(context.getString(R.string.place_reminder_type_checklist))
        val header = title.fetchSemanticsNode().boundsInRoot
        val media = compose.onNodeWithTag("place_reminder_media_stack").fetchSemanticsNode().boundsInRoot
        assertTrue(header.top < media.bottom)
        assertTrue(header.right <= media.left)
        compose.onNodeWithText(context.getString(R.string.place_reminder_new_item)).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value = reminder(checklist = true, itemCount = 2) }
        compose.onAllNodesWithTag("place_reminder_media_stack").assertCountEquals(1)
        compose.onNodeWithText("待辦項目 2").performScrollTo().assertIsDisplayed()
    }

    @Test fun resetButtonFillsChecklistWidthAndFollowsAddInput() {
        state.value = reminder(text = "", checklist = true, itemCount = 2)
        show()
        val reset = compose.onNodeWithTag("place_reminder_reset_checklist")
        reset.performScrollTo().assertWidthIsEqualTo(280.dp).assertIsDisplayed()
        val input = compose.onNodeWithText(context.getString(R.string.place_reminder_new_item))
            .fetchSemanticsNode().boundsInRoot
        assertTrue(reset.fetchSemanticsNode().boundsInRoot.top > input.bottom)
        reset.performClick()
        compose.runOnIdle { assertEquals(1, resetCount) }
        save("detail-checklist-bottom-reset.png")
    }

    @Test fun largeFontLongContentAndChecklistRemainReachableOnSmallPhone() {
        state.value = reminder(text = "提醒內容\n".repeat(20), checklist = true, itemCount = 12)
        show(fontScale = 2f, height = 640)
        assertTrue(scrollRange().maxValue() > 0f)
        compose.onNodeWithText("待辦項目 12").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.place_reminder_new_item)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("place_reminder_media_stack").performScrollTo().assertHeightIsEqualTo(144.dp)
        save("detail-large-font.png")
    }

    @Test fun attachmentOnlyUsesMinimumHeightAndScrollsWhenSpaceIsInsufficient() {
        state.value = reminder(text = "")
        show(height = 320)
        compose.onNodeWithTag("place_reminder_media_stack").assertHeightIsEqualTo(160.dp)
        assertTrue(scrollRange().maxValue() > 0f)
        compose.onNodeWithTag("place_reminder_media_front").performScrollTo().assertIsDisplayed()
    }

    @Test fun exactContentAndActionBarFitDoesNotLeaveExtraScrollRange() {
        state.value = reminder(text = "", checklist = true, itemCount = 0)
        show(withActionBar = true)
        val content = compose.onNodeWithTag("place_reminder_detail_group").fetchSemanticsNode().boundsInRoot
        val bar = compose.onNodeWithTag("detail_action_bar").fetchSemanticsNode().boundsInRoot
        val fittingHeight = with(compose.density) { (content.bottom + bar.height).toDp().value }
        compose.runOnIdle { viewportHeight.value = kotlin.math.ceil(fittingHeight).toInt() }
        assertEquals(0f, scrollRange().maxValue(), 1f)
    }

    @Test fun scrolledContentEndsExactlyAboveMeasuredActionBar() {
        state.value = reminder(text = "", checklist = true, itemCount = 12)
        show(withActionBar = true, fontScale = 2f)
        compose.onNodeWithTag("place_reminder_detail_scroll")
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        val content = compose.onNodeWithTag("place_reminder_detail_group").fetchSemanticsNode().boundsInRoot
        val bar = compose.onNodeWithTag("detail_action_bar").fetchSemanticsNode().boundsInRoot
        assertEquals(bar.top, content.bottom, 1f)
    }

    @Test fun landscapeRetainsOriginalMediaBelowContent() {
        show(landscape = true)
        compose.onNodeWithText(context.getString(R.string.place_reminder_attachments)).performScrollTo().assertIsDisplayed()
        val stack = compose.onNodeWithTag("place_reminder_media_stack").fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText("記得帶環保袋").fetchSemanticsNode().boundsInRoot
        assertTrue(stack.top > text.bottom)
    }
}
