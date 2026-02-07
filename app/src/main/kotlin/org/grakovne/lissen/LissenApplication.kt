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
  lateinit var runningComponents: Set<@JvmSuppressWildcards RunningComponent>

  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler(
      org.grakovne.lissen.common
        .CrashHandler(this, defaultHandler),
    )

    runningComponents.forEach {
      try {
        it.onCreate()
      } catch (ex: Exception) {
        Timber.e("Unable to register Running component due to: ${ex.message}")
      }
    }
  }

  companion object {
    lateinit var appContext: Context
      private set
  }
}
