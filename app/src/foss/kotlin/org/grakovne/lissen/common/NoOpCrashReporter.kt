package org.grakovne.lissen.common

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpCrashReporter
  @Inject
  constructor() : CrashReporter {
    override fun setCollectionEnabled(enabled: Boolean) {
      // Do nothing
    }

    override fun recordException(exception: Throwable) {
      // Do nothing
    }
  }
