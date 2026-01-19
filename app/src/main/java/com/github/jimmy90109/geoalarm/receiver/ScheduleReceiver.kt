package com.github.jimmy90109.geoalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.jimmy90109.geoalarm.service.ScheduleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduleManager = ScheduleManager(context)
            scope.launch {
                scheduleManager.rescheduleAll()
            }
        } else if (intent.action == ScheduleManager.ACTION_SCHEDULE_TRIGGER) {
            val scheduleId = intent.getStringExtra(ScheduleManager.EXTRA_SCHEDULE_ID) ?: return
             val scheduleManager = ScheduleManager(context)
            scope.launch {
                scheduleManager.handleScheduleTrigger(scheduleId)
            }
        }
    }
}
