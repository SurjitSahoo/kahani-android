package org.grakovne.lissen.common

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpCrashReporter
  @Inject
  constructor() : CrashReporter {
    override fun setCollectionEnabled(enabled: Boolean) {
      // Do nothing in FOSS
    }

    override fun recordException(ex: Throwable) {
      Timber.e(ex, "Crash reported (FOSS mode - no remote reporting)")
    }
  }
