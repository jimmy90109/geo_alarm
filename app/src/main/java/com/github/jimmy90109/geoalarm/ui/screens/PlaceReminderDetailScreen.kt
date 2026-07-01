package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage

import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachment
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachmentType
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.data.PlaceTriggerType
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditControlMode
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditStep
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderPermissionState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val RadiusOptions = listOf(100, 150, 200, 300)
private val DwellOptions = listOf(1, 3, 5, 10)
private val CooldownOptions = listOf(60, 180, 360, 1440)
private const val MediaPreviewTransitionMillis = 360
private const val MediaPreviewReturnMillis = 260

private data class MediaPreviewSelection(
    val attachment: PlaceReminderAttachment,
    val sourceBounds: Rect,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceReminderDetailScreen(
    viewModel: PlaceReminderDetailViewModel,
    reminderId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val reminderWithItems by viewModel.reminder.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedPreview by remember { mutableStateOf<MediaPreviewSelection?>(null) }
    var permissionState by remember {
        mutableStateOf(PlaceReminderListViewModel.permissionState(context))
    }
    var pendingEnableReminderId by remember { mutableStateOf<String?>(null) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    lateinit var requestEnableReminder: (String) -> Unit

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionState = PlaceReminderListViewModel.permissionState(context)
        if (permissionState.hasNotifications) {
            pendingEnableReminderId?.let { requestEnableReminder(it) }
        } else {
            pendingEnableReminderId = null
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionState = PlaceReminderListViewModel.permissionState(context)
        if (permissionState.hasPreciseLocation) {
            pendingEnableReminderId?.let { requestEnableReminder(it) }
        } else {
            pendingEnableReminderId = null
        }
    }

    fun refreshPermissions() {
        permissionState = PlaceReminderListViewModel.permissionState(context)
    }

    requestEnableReminder = { id ->
        refreshPermissions()
        when {
            !permissionState.hasPreciseLocation -> {
                pendingEnableReminderId = id
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
            !permissionState.hasNotifications -> {
                pendingEnableReminderId = id
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            !permissionState.hasBackgroundLocation -> {
                pendingEnableReminderId = id
                showBackgroundLocationDialog = true
            }
            !permissionState.isLocationServiceEnabled -> {
                pendingEnableReminderId = id
                context.openLocationSettings()
            }
            else -> {
                pendingEnableReminderId = null
                viewModel.setEnabled(id, true)
            }
        }
    }

    LaunchedEffect(reminderId) {
        viewModel.load(reminderId)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlaceReminderDetailEffect.NavigateBack -> onBack()
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                pendingEnableReminderId?.let { id ->
                    if (permissionState.canEnableReminder) {
                        pendingEnableReminderId = null
                        viewModel.setEnabled(id, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val current = reminderWithItems
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current?.reminder?.title ?: stringResource(R.string.place_reminder_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEdit(reminderId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.place_reminder_edit_action))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                )
            },
        ) { innerPadding ->
            if (current == null) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.place_reminder_not_found))
                }
            } else {
                val reminder = current.reminder
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlarmIconBadge(iconKey = reminder.iconKey)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(reminder.placeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = reminder.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(triggerText(reminder.triggerType, reminder.dwellMinutes))
                                }
                                Switch(
                                    checked = reminder.enabled,
                                    onCheckedChange = {
                                        if (it) {
                                            requestEnableReminder(reminder.id)
                                        } else {
                                            pendingEnableReminderId = null
                                            viewModel.setEnabled(reminder.id, false)
                                        }
                                    },
                                )
                            }
                            reminder.lastTriggeredAt?.let {
                                Text(
                                    text = stringResource(R.string.place_reminder_last_triggered, formatTime(it)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (reminder.content.isNotBlank()) {
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.place_reminder_content),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = reminder.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    if (reminder.type == PlaceReminderType.CHECKLIST) {
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.place_reminder_type_checklist),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.resetChecklist(reminder.id) }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.place_reminder_reset_checklist))
                                    }
                                }
                                current.sortedItems.forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = item.checked,
                                            onCheckedChange = { viewModel.setItemChecked(item, it) },
                                        )
                                        Text(item.text, modifier = Modifier.weight(1f))
                                    }
                                }
                                HorizontalDivider()
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newItemText,
                                        onValueChange = { newItemText = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        placeholder = { Text(stringResource(R.string.place_reminder_new_item)) },
                                    )
                                    IconButton(onClick = {
                                        viewModel.addItem(reminder.id, newItemText)
                                        newItemText = ""
                                    }) {
                                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.place_reminder_add_item))
                                    }
                                }
                            }
                        }
                    }
                    if (current.sortedAttachments.isNotEmpty()) {
                        PlaceReminderAttachmentGrid(
                            reminderWithItems = current,
                            hiddenAttachmentId = selectedPreview?.attachment?.id,
                            onAttachmentClick = { attachment, sourceBounds ->
                                selectedPreview = MediaPreviewSelection(attachment, sourceBounds)
                            },
                            onAttachmentDelete = { viewModel.deleteAttachment(it) },
                        )
                    }
                }
            }
        }

        selectedPreview?.let {
            PlaceReminderMediaPreviewOverlay(
                selection = it,
                onDismiss = { selectedPreview = null },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.place_reminder_delete_title)) },
            text = { Text(stringResource(R.string.place_reminder_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showBackgroundLocationDialog) {
        BackgroundLocationPermissionDialog(
            context = context,
            onDismiss = {
                pendingEnableReminderId = null
                showBackgroundLocationDialog = false
            },
            onOpenSettings = {
                showBackgroundLocationDialog = false
            },
        )
    }

}

@Composable
private fun PlaceReminderAttachmentGrid(
    reminderWithItems: PlaceReminderWithItems,
    hiddenAttachmentId: String?,
    onAttachmentClick: (PlaceReminderAttachment, Rect) -> Unit,
    onAttachmentDelete: (String) -> Unit,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.place_reminder_attachments),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reminderWithItems.sortedAttachments, key = { it.id }) { attachment ->
                    val isVideo = attachment.type == PlaceReminderAttachmentType.VIDEO
                    val context = LocalContext.current
                    val imageRequest = remember(attachment.localPath, isVideo) {
                        ImageRequest.Builder(context)
                            .data(attachment.localPath)
                            .crossfade(true)
                            .apply {
                                if (isVideo) {
                                    decoderFactory(VideoFrameDecoder.Factory())
                                }
                            }
                            .build()
                    }
                    var itemBounds by remember(attachment.id) { mutableStateOf<Rect?>(null) }
                    Card(
                        modifier = Modifier
                            .height(100.dp)
                            .onGloballyPositioned { coordinates ->
                                itemBounds = coordinates.boundsInRoot()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = hiddenAttachmentId == null) {
                                itemBounds?.let { onAttachmentClick(attachment, it) }
                            }
                            .graphicsLayer {
                                alpha = if (attachment.id == hiddenAttachmentId) 0f else 1f
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = attachment.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (isVideo) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Icon(
                                        Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceReminderMediaPreviewOverlay(
    selection: MediaPreviewSelection,
    onDismiss: () -> Unit,
) {
    val attachment = selection.attachment
    val context = LocalContext.current
    val isVideo = attachment.type == PlaceReminderAttachmentType.VIDEO
    val imageRequest = remember(attachment.localPath, isVideo) {
        ImageRequest.Builder(context)
            .data(attachment.localPath)
            .crossfade(true)
            .apply {
                if (isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                }
            }
            .build()
    }
    val storedAspectRatio = remember(attachment.width, attachment.height) {
        val width = attachment.width?.takeIf { it > 0 }?.toFloat()
        val height = attachment.height?.takeIf { it > 0 }?.toFloat()
        if (width != null && height != null) width / height else null
    }
    var decodedAspectRatio by remember(attachment.id) {
        mutableStateOf(storedAspectRatio)
    }
    val mediaAspectRatio = decodedAspectRatio ?: 1f

    LaunchedEffect(attachment.id, attachment.localPath, isVideo, storedAspectRatio) {
        if (storedAspectRatio == null) {
            decodedAspectRatio = resolveMediaAspectRatio(
                localPath = attachment.localPath,
                isVideo = isVideo,
            ) ?: 1f
        }
    }

    val scope = rememberCoroutineScope()
    val transitionProgress = remember(attachment.id) { Animatable(0f) }
    val dragOffsetY = remember(attachment.id) { Animatable(0f) }
    var isDragging by remember(attachment.id) { mutableStateOf(false) }
    var hasDismissed by remember(attachment.id) { mutableStateOf(false) }

    fun requestDismiss() {
        if (hasDismissed) return
        hasDismissed = true
        scope.launch {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(MediaPreviewReturnMillis),
            )
            onDismiss()
        }
    }

    BackHandler(onBack = ::requestDismiss)

    LaunchedEffect(attachment.id) {
        transitionProgress.snapTo(0f)
        dragOffsetY.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(MediaPreviewTransitionMillis),
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val horizontalPaddingPx = with(density) { 12.dp.toPx() }
        val verticalPaddingPx = with(density) { 24.dp.toPx() }
        val dismissThresholdPx = maxHeightPx * 0.16f
        val dismissProgress by remember(maxHeightPx) {
            derivedStateOf {
                (abs(dragOffsetY.value) / (maxHeightPx * 0.34f)).coerceIn(0f, 1f)
            }
        }
        val viewerScale by remember {
            derivedStateOf {
                1f - (dismissProgress * 0.16f)
            }
        }
        val targetBounds = remember(
            maxWidthPx,
            maxHeightPx,
            horizontalPaddingPx,
            verticalPaddingPx,
            mediaAspectRatio,
        ) {
            fitRectInContainer(
                containerWidth = maxWidthPx,
                containerHeight = maxHeightPx,
                horizontalPadding = horizontalPaddingPx,
                verticalPadding = verticalPaddingPx,
                aspectRatio = mediaAspectRatio,
            )
        }
        val draggedTargetBounds = remember(targetBounds, viewerScale, dragOffsetY.value) {
            targetBounds
                .scaleAroundCenter(viewerScale)
                .translate(y = dragOffsetY.value)
        }
        val animatedBounds = remember(selection.sourceBounds, draggedTargetBounds, transitionProgress.value) {
            lerp(selection.sourceBounds, draggedTargetBounds, transitionProgress.value)
        }
        val animatedCornerRadius = with(density) {
            lerpFloat(
                start = 12.dp.toPx(),
                end = (8.dp + (24.dp * dismissProgress)).toPx(),
                fraction = transitionProgress.value,
            ).toDp()
        }
        val animatedWidth = with(density) { animatedBounds.width.toDp() }
        val animatedHeight = with(density) { animatedBounds.height.toDp() }
        val controlsAlpha by animateFloatAsState(
            targetValue = if (transitionProgress.value > 0.98f && !isDragging && !hasDismissed) {
                1f - dismissProgress
            } else {
                0f
            },
            animationSpec = tween(120),
            label = "mediaPreviewControlsAlpha",
        )
        val imageContentScale = if (transitionProgress.value > 0.96f) {
            ContentScale.Fit
        } else {
            ContentScale.Crop
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.94f * transitionProgress.value * (1f - dismissProgress),
                    ),
                )
                .pointerInput(attachment.id) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            if (transitionProgress.value < 1f) return@detectVerticalDragGestures
                            isDragging = true
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (transitionProgress.value < 1f) return@detectVerticalDragGestures
                            change.consume()
                            scope.launch {
                                dragOffsetY.snapTo(dragOffsetY.value + dragAmount)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (abs(dragOffsetY.value) > dismissThresholdPx) {
                                requestDismiss()
                            } else {
                                scope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = animatedBounds.left
                        translationY = animatedBounds.top
                    }
                    .size(width = animatedWidth, height = animatedHeight)
                    .clip(RoundedCornerShape(animatedCornerRadius)),
            ) {
                if (transitionProgress.value > 0.98f) {
                    ZoomableAsyncImage(
                        model = imageRequest,
                        contentDescription = attachment.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = attachment.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = imageContentScale,
                    )
                }
                if (isVideo) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .graphicsLayer {
                                alpha = transitionProgress.value * (1f - dismissProgress)
                            },
                    )
                }
            }
            IconButton(
                onClick = ::requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .graphicsLayer {
                        alpha = controlsAlpha
                    },
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                )
            }
        }
    }
}

private suspend fun resolveMediaAspectRatio(
    localPath: String,
    isVideo: Boolean,
): Float? = withContext(Dispatchers.IO) {
    if (isVideo) {
        resolveVideoAspectRatio(localPath)
    } else {
        resolveImageAspectRatio(localPath)
    }
}

private fun fitRectInContainer(
    containerWidth: Float,
    containerHeight: Float,
    horizontalPadding: Float,
    verticalPadding: Float,
    aspectRatio: Float,
): Rect {
    val availableWidth = (containerWidth - (horizontalPadding * 2f)).coerceAtLeast(1f)
    val availableHeight = (containerHeight - (verticalPadding * 2f)).coerceAtLeast(1f)
    val availableAspectRatio = availableWidth / availableHeight
    val width: Float
    val height: Float
    if (availableAspectRatio > aspectRatio) {
        height = availableHeight
        width = height * aspectRatio
    } else {
        width = availableWidth
        height = width / aspectRatio
    }
    val left = (containerWidth - width) / 2f
    val top = (containerHeight - height) / 2f
    return Rect(left = left, top = top, right = left + width, bottom = top + height)
}

private fun lerp(start: Rect, end: Rect, fraction: Float): Rect {
    val coercedFraction = fraction.coerceIn(0f, 1f)
    return Rect(
        left = lerpFloat(start.left, end.left, coercedFraction),
        top = lerpFloat(start.top, end.top, coercedFraction),
        right = lerpFloat(start.right, end.right, coercedFraction),
        bottom = lerpFloat(start.bottom, end.bottom, coercedFraction),
    )
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float =
    start + ((end - start) * fraction.coerceIn(0f, 1f))

private fun Rect.scaleAroundCenter(scale: Float): Rect {
    val center = center
    val scaledWidth = width * scale
    val scaledHeight = height * scale
    return Rect(
        left = center.x - (scaledWidth / 2f),
        top = center.y - (scaledHeight / 2f),
        right = center.x + (scaledWidth / 2f),
        bottom = center.y + (scaledHeight / 2f),
    )
}

private fun Rect.translate(x: Float = 0f, y: Float = 0f): Rect =
    Rect(left = left + x, top = top + y, right = right + x, bottom = bottom + y)

private fun resolveImageAspectRatio(localPath: String): Float? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(localPath, options)
    val width = options.outWidth.takeIf { it > 0 } ?: return null
    val height = options.outHeight.takeIf { it > 0 } ?: return null
    return width.toFloat() / height.toFloat()
}

private fun resolveVideoAspectRatio(localPath: String): Float? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(localPath)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return null
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return null
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?: 0
        if (rotation == 90 || rotation == 270) {
            height.toFloat() / width.toFloat()
        } else {
            width.toFloat() / height.toFloat()
        }
    } finally {
        runCatching { retriever.release() }
    }
}
