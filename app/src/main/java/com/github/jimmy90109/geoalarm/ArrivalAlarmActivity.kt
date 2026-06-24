package com.github.jimmy90109.geoalarm

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.github.jimmy90109.geoalarm.appactions.AlarmTurnOffUseCase
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArrivalAlarmActivity : AppCompatActivity() {
    @Inject
    lateinit var alarmTurnOffUseCase: AlarmTurnOffUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()

        val alarmId = intent.getStringExtra(GeoAlarmService.EXTRA_ALARM_ID).orEmpty()
        setContent {
            GeoAlarmTheme {
                var isStopping by remember { mutableStateOf(false) }

                ArrivalAlarmContent(
                    isStopping = isStopping,
                    canStop = alarmId.isNotEmpty(),
                    onStopClick = {
                        isStopping = true
                        lifecycleScope.launch {
                            alarmTurnOffUseCase(alarmId, trackArrivedTurnOff = true)
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ArrivalAlarmContent(
    isStopping: Boolean,
    canStop: Boolean,
    onStopClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "arrivalPulse")
    val titleAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.8f at 0
                0.4f at 500
                0.8f at 700
                0.4f at 1200
                0.8f at 1400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "arrivalTitleAlpha",
    )
    val titleScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1.03f at 0
                1f at 500
                1.03f at 700
                1f at 1200
                1.03f at 1400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "arrivalTitleScale",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val arrivalFontSize = (maxWidth.value * 0.25f).coerceIn(72f, 140f).sp
        val arrivalLineHeight = (maxWidth.value * 0.27f).coerceIn(78f, 150f).sp

        ArrivalBranding(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.10f),
            iconSize = 64.dp,
            textStyle = MaterialTheme.typography.titleLarge,
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.arrived_status),
                style = MaterialTheme.typography.displayLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = arrivalFontSize,
                    lineHeight = arrivalLineHeight,
                ),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    scaleX = titleScale
                    scaleY = titleScale
                },
            )
        }

        Button(
            enabled = canStop && !isStopping,
            onClick = onStopClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = maxHeight * 0.1f)
                .height(64.dp),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text(
                text = stringResource(R.string.notification_turn_off),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArrivalBranding(
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 64.dp,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize),
            colorFilter = ColorFilter.tint(contentColor),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = textStyle,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(
    name = "Arrival alarm - portrait",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun ArrivalAlarmContentPortraitPreview() {
    GeoAlarmTheme {
        ArrivalAlarmContent(
            isStopping = false,
            canStop = true,
            onStopClick = {},
        )
    }
}

@Preview(
    name = "Arrival alarm - landscape",
    showBackground = true,
    widthDp = 852,
    heightDp = 393,
)
@Composable
private fun ArrivalAlarmContentLandscapePreview() {
    GeoAlarmTheme {
        ArrivalAlarmContent(
            isStopping = false,
            canStop = true,
            onStopClick = {},
        )
    }
}
