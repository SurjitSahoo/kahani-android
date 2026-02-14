package org.grakovne.lissen.common

interface CrashReporter {
  fun setCollectionEnabled(enabled: Boolean)

  fun recordException(ex: Throwable)
}
