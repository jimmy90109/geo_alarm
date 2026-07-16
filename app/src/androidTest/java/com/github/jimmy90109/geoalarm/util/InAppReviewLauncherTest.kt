package com.github.jimmy90109.geoalarm.util

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jimmy90109.geoalarm.MainActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InAppReviewLauncherTest {
    @Test
    fun fakeReviewManagerCompletesLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val completed = CountDownLatch(1)

            scenario.onActivity { activity ->
                val launcher = InAppReviewLauncher(
                    reviewManagerProvider = object : ReviewManagerProvider {
                        override fun create(context: android.content.Context): ReviewManager =
                            FakeReviewManager(context)
                    }
                )
                launcher.launch(activity) { completed.countDown() }
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun requestFailureCompletesWithoutLaunchingReview() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val completed = CountDownLatch(1)

            scenario.onActivity { activity ->
                val launcher = InAppReviewLauncher(
                    reviewManagerProvider = object : ReviewManagerProvider {
                        override fun create(context: android.content.Context): ReviewManager =
                            FailingReviewManager
                    }
                )
                launcher.launch(activity) { completed.countDown() }
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
        }
    }

    private object FailingReviewManager : ReviewManager {
        override fun requestReviewFlow(): Task<ReviewInfo> =
            Tasks.forException(IllegalStateException("Play Store unavailable"))

        override fun launchReviewFlow(
            activity: android.app.Activity,
            reviewInfo: ReviewInfo,
        ): Task<Void> = error("launchReviewFlow must not be called")
    }
}
