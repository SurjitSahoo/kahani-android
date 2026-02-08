package org.grakovne.lissen.common

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CrashReporterModule {
  @Binds
  fun bindCrashReporter(reporter: FirebaseCrashReporter): CrashReporter
}
