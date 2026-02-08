package org.grakovne.lissen.common

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCrashReporter
  @Inject
  constructor() : CrashReporter {
    override fun setCollectionEnabled(enabled: Boolean) {
      FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

    override fun recordException(ex: Throwable) {
      FirebaseCrashlytics.getInstance().recordException(ex)
    }
  }
