package com.github.jimmy90109.geoalarm.ads

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface AdsEntitlementRepository {
    val hasAdsRemoved: Flow<Boolean>
}

@Singleton
class DefaultAdsEntitlementRepository @Inject constructor() : AdsEntitlementRepository {
    override val hasAdsRemoved: Flow<Boolean> = flowOf(false)
}
