package org.grakovne.lissen.analytics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsModule {
  @Binds
  fun bindAnalyticsTracker(tracker: NoOpAnalyticsTracker): AnalyticsTracker
}
