package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.cos
import kotlin.math.log2
import kotlinx.coroutines.delay

private val PlaceReminderPreviewMapHeight = 180.dp
private const val PlaceReminderPreviewMapDeferMillis = 180L

@Composable
internal fun PlaceReminderSelectedPlaceSection(
    state: PlaceReminderEditUiState,
    onSelectPlace: () -> Unit,
) {
    if (state.selectedPosition == null) {
        Button(
            onClick = onSelectPlace,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
        ) {
            Text(stringResource(R.string.select_shared_place))
        }
        return
    }
    val position = state.selectedPosition
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val mapHeightPx = with(density) { PlaceReminderPreviewMapHeight.toPx() }
    val previewZoom = remember(position, state.radiusMeters, mapHeightPx) {
        previewZoomForRadius(
            latitude = position.latitude,
            radiusMeters = state.radiusMeters,
            mapHeightPx = mapHeightPx,
        )
    }
    var showPreviewMap by remember(position, state.radiusMeters, darkTheme) { mutableStateOf(false) }
    LaunchedEffect(position, state.radiusMeters, darkTheme) {
        withFrameNanos { }
        delay(PlaceReminderPreviewMapDeferMillis)
        showPreviewMap = true
    }
    val previewMapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = false,
            scrollGesturesEnabledDuringRotateOrZoom = false,
            tiltGesturesEnabled = false,
            zoomGesturesEnabled = false,
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectPlace),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlarmIconBadge(iconKey = state.selectedIconKey)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.placeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.address?.isNotBlank() == true && state.address != state.placeName) {
                        Text(
                            text = state.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.place_reminder_edit_place))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlaceReminderPreviewMapHeight),
            ) {
                if (showPreviewMap) {
                    var mapProperties by remember { mutableStateOf(MapProperties()) }
                    LaunchedEffect(darkTheme) {
                        mapProperties = if (darkTheme) {
                            MapProperties(
                                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                                    context,
                                    R.raw.map_style_dark,
                                ),
                            )
                        } else {
                            MapProperties(mapStyleOptions = null)
                        }
                    }
                    val cameraPositionState = rememberCameraPositionState {
                        this.position = CameraPosition.fromLatLngZoom(position, previewZoom)
                    }
                    LaunchedEffect(position, previewZoom) {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, previewZoom))
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = previewMapUiSettings,
                    ) {
                        Marker(state = MarkerState(position = position), title = state.placeName)
                        Circle(
                            center = position,
                            radius = state.radiusMeters.toDouble(),
                            strokeColor = Color(0xFF607D8B).copy(alpha = 0.8f),
                            strokeWidth = 2f,
                            fillColor = Color(0xFF607D8B).copy(alpha = 0.14f),
                        )
                    }
                } else {
                    PlaceReminderPreviewMapPlaceholder(modifier = Modifier.matchParentSize())
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onSelectPlace),
                )
            }
        }
    }
}

@Composable
private fun PlaceReminderPreviewMapPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

private fun previewZoomForRadius(
    latitude: Double,
    radiusMeters: Int,
    mapHeightPx: Float,
): Float {
    val radius = radiusMeters.coerceAtLeast(50)
    val targetDiameterPx = mapHeightPx * 0.72f
    val metersPerPixel = (radius * 2f) / targetDiameterPx.coerceAtLeast(1f)
    val latitudeScale = cos(Math.toRadians(latitude)).coerceAtLeast(0.2)
    val zoom = log2((156543.03392 * latitudeScale) / metersPerPixel)
    return zoom.toFloat().coerceIn(10f, 18f)
}
