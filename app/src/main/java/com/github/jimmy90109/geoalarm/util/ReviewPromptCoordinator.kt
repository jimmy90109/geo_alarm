package com.github.jimmy90109.geoalarm.util

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewPromptCoordinator @Inject constructor() {
    private val pending = AtomicBoolean(false)

    fun markPending() {
        pending.set(true)
    }

    fun hasPending(): Boolean = pending.get()

    fun consumePending(): Boolean = pending.compareAndSet(true, false)
}
