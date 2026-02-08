package org.grakovne.lissen.analytics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.grakovne.lissen.common.RunningComponent

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsModule {
  @Binds
  @IntoSet
  fun bindClarityComponent(component: ClarityComponent): RunningComponent
}
