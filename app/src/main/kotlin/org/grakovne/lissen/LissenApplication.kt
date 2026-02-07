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

  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext

    // Initialize core services first
    try {
      if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
      }

      val isCrashReportingEnabled = preferences.getCrashReportingEnabled()
      com.google.firebase.crashlytics.FirebaseCrashlytics
        .getInstance()
        .setCrashlyticsCollectionEnabled(isCrashReportingEnabled)

      val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

      Thread.setDefaultUncaughtExceptionHandler(
        org.grakovne.lissen.common
          .CrashHandler(this, defaultHandler),
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
        com.google.firebase.crashlytics.FirebaseCrashlytics
          .getInstance()
          .recordException(ex)
      }
    }
  }

  companion object {
    lateinit var appContext: Context
      private set
  }
}
