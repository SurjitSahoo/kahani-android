package org.grakovne.lissen.analytics

import android.content.Context
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.grakovne.lissen.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MicrosoftClarityTracker
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
  ) : AnalyticsTracker {
    override fun trackEvent(
      name: String,
      value: String?,
    ) {
      if (value == null) {
        Clarity.sendCustomEvent(name)
      } else {
        Clarity.sendCustomEvent("$name: $value")
      }
    }

    override fun setUser(id: String) {
      Clarity.setCustomUserId(id)
    }

    override fun updateConsent(accepted: Boolean) {
      if (accepted) {
        val config =
          ClarityConfig(
            projectId = BuildConfig.CLARITY_PROJECT_ID,
          )
        Clarity.initialize(context, config)
      } else {
        Clarity.pause()
      }
    }
  }
