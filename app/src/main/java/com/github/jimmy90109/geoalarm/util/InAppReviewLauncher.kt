package com.github.jimmy90109.geoalarm.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface ReviewManagerProvider {
    fun create(context: Context): ReviewManager
}

class PlayReviewManagerProvider @Inject constructor() : ReviewManagerProvider {
    override fun create(context: Context): ReviewManager = ReviewManagerFactory.create(context)
}

@Singleton
class InAppReviewLauncher @Inject constructor(
    private val reviewManagerProvider: ReviewManagerProvider,
) {
    private val requestInProgress = AtomicBoolean(false)

    fun launch(activity: Activity, onComplete: () -> Unit = {}) {
        if (!requestInProgress.compareAndSet(false, true)) {
            onComplete()
            return
        }

        val complete = {
            requestInProgress.set(false)
            onComplete()
        }

        val manager = runCatching {
            reviewManagerProvider.create(activity)
        }.getOrElse {
            complete()
            return
        }

        runCatching {
            manager.requestReviewFlow().addOnCompleteListener { requestTask ->
                if (!requestTask.isSuccessful) {
                    complete()
                    return@addOnCompleteListener
                }

                runCatching {
                    manager.launchReviewFlow(activity, requestTask.result)
                        .addOnCompleteListener { complete() }
                }.onFailure {
                    complete()
                }
            }
        }.onFailure {
            complete()
        }
    }
}
