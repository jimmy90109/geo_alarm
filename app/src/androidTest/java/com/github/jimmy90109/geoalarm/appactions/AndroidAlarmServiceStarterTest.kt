package com.github.jimmy90109.geoalarm.appactions

import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jimmy90109.geoalarm.MainActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAlarmServiceStarterTest {
    @Test
    fun stopWaitsForServiceCleanupAcknowledgment() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val completed = CountDownLatch(1)
            val cleanupConfirmed = AtomicBoolean(false)

            scenario.onActivity { activity ->
                activity.lifecycleScope.launch {
                    cleanupConfirmed.set(
                        AndroidAlarmServiceStarter(activity.applicationContext)
                            .stopCurrentAlarm("ack-test-alarm")
                    )
                    completed.countDown()
                }
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertTrue(cleanupConfirmed.get())
        }
    }
}
