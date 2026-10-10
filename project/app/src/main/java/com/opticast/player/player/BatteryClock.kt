package com.opticast.player.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatteryClock(
    modifier: Modifier = Modifier,
    showBattery: Boolean = true,
    showClock: Boolean = true
) {
    val context = LocalContext.current
    var batteryPct by remember { mutableIntStateOf(getBatteryPct(context)) }
    var timeText by remember { mutableStateOf(getTimeText()) }

    LaunchedEffect(Unit) {
        while (true) {
            batteryPct = getBatteryPct(context)
            timeText = getTimeText()
            delay(60_000L)
        }
    }

    if (!showBattery && !showClock) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showBattery) {
                Text(
                    text = "$batteryPct%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (showBattery && showClock) {
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (showClock) {
                Text(
                    text = timeText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun getBatteryPct(context: Context): Int {
    return try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (pct > 0) pct else {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        }
    } catch (_: Exception) {
        0
    }
}

private fun getTimeText(): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date())
    } catch (_: Exception) {
        ""
    }
}
