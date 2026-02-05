package org.grakovne.lissen.analytics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.grakovne.lissen.BuildConfig
import org.grakovne.lissen.common.RunningComponent
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClarityComponent
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
    private val preferences: LissenSharedPreferences,
    private val clarityTracker: ClarityTracker,
  ) : RunningComponent,
    Application.ActivityLifecycleCallbacks {
    override fun onCreate() {
      (context as? Application)?.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(
      activity: Activity,
      savedInstanceState: Bundle?,
    ) {
      // Initialize Clarity only once with the first activity
      if (initialized) return

      Timber.d("Initializing Microsoft Clarity with activity: ${activity.javaClass.simpleName}")
      val config = ClarityConfig(BuildConfig.CLARITY_PROJECT_ID)
      Clarity.initialize(activity, config)

      initialized = true
      reidentifyUser()
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(
      activity: Activity,
      outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private var initialized = false

    private fun reidentifyUser() {
      if (preferences.hasCredentials()) {
        val username = preferences.getUsername() ?: return
        val host = preferences.getHost() ?: return

        // Combine host and username for a unique identifier across servers
        val identifier = "$username@$host"
        clarityTracker.setUser(identifier)
      } else {
        // Fallback to device ID if not logged in
        clarityTracker.setUser(preferences.getDeviceId())
      }
    }
  }
