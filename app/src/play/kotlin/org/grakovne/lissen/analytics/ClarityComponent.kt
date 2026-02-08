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
      if (BuildConfig.DEBUG) {
        Timber.d("Skip Microsoft Clarity initialization for debug build")
        return
      }

      (context as? Application)?.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(
      activity: Activity,
      savedInstanceState: Bundle?,
    ) {
      // Initialize Clarity only once with the first activity
      if (isClarityInitialized) return

      Timber.d("Initializing Microsoft Clarity with activity: ${activity.javaClass.simpleName}")
      val config = ClarityConfig(BuildConfig.CLARITY_PROJECT_ID)
      Clarity.initialize(activity, config)
      isClarityInitialized = true

      applyConsent()
      reidentifyUser()
    }

    fun updateConsent(accepted: Boolean) {
      if (!isClarityInitialized) return
      Clarity.consent(accepted, accepted)
    }

    private fun applyConsent() {
      val consent = preferences.getAnalyticsConsentState() ?: false
      Clarity.consent(consent, consent)
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

    private var isClarityInitialized = false

    private fun reidentifyUser() {
      if (preferences.hasCredentials()) {
        val username = preferences.getUsername() ?: return
        val host = preferences.getHost() ?: return

        // Combine host and username for a unique identifier across servers
        val rawIdentifier = "$username@$host"
        val hashedIdentifier = hashIdentifier(rawIdentifier)

        clarityTracker.setUser(hashedIdentifier)
      } else {
        // Fallback to device ID if not logged in
        clarityTracker.setUser(preferences.getDeviceId())
      }
    }

    private fun hashIdentifier(input: String): String {
      val digest = java.security.MessageDigest.getInstance("SHA-256")
      val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
      return hashBytes.joinToString("") { "%02x".format(it) }
    }
  }
