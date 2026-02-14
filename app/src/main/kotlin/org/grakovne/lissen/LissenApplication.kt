package org.grakovne.lissen

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import org.grakovne.lissen.common.RunningComponent
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LissenApplication : Application() {
  @Inject
  lateinit var preferences: org.grakovne.lissen.persistence.preferences.LissenSharedPreferences

  @Inject
  lateinit var runningComponents: Set<@JvmSuppressWildcards RunningComponent>

  @Inject
  lateinit var crashReporter: org.grakovne.lissen.common.CrashReporter

  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext

    // Initialize core services first
    try {
      if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
      }

      val isCrashReportingEnabled = preferences.getCrashReportingEnabled()
      crashReporter.setCollectionEnabled(isCrashReportingEnabled)

      val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

      Thread.setDefaultUncaughtExceptionHandler(
        org.grakovne.lissen.common
          .CrashHandler(this, crashReporter, defaultHandler),
      )
    } catch (e: Exception) {
      // Fallback logging if core services fail
      android.util.Log.e("LissenApplication", "Failed to initialize core services", e)
    }

    // Initialize components with individual error handling
    runningComponents.forEach {
      try {
        it.onCreate()
      } catch (ex: Exception) {
        Timber.e(ex, "Unable to register Running component: ${ex.message}")
        crashReporter.recordException(ex)
      }
    }
  }

  companion object {
    lateinit var appContext: Context
      private set
  }
}
