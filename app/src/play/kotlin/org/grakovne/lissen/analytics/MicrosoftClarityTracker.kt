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
    private var clarityInitialized = false
    private val eventQueue = mutableListOf<Pair<String, String?>>()
    private var pendingUserId: String? = null

    override fun trackEvent(
      name: String,
      value: String?,
    ) {
      if (clarityInitialized) {
        sendEventToClarity(name, value)
      } else {
        eventQueue.add(name to value)
      }
    }

    override fun setUser(id: String) {
      if (clarityInitialized) {
        Clarity.setCustomUserId(id)
      } else {
        pendingUserId = id
      }
    }

    override fun updateConsent(accepted: Boolean) {
      if (accepted) {
        if (!clarityInitialized) {
          val config =
            ClarityConfig(
              projectId = BuildConfig.CLARITY_PROJECT_ID,
            )
          Clarity.initialize(context, config)
          clarityInitialized = true
          flushQueues()
        }
      } else {
        if (clarityInitialized) {
          Clarity.pause()
        }
      }
    }

    private fun sendEventToClarity(
      name: String,
      value: String?,
    ) {
      if (value == null) {
        Clarity.sendCustomEvent(name)
      } else {
        Clarity.sendCustomEvent("$name: $value")
      }
    }

    private fun flushQueues() {
      pendingUserId?.let {
        Clarity.setCustomUserId(it)
        pendingUserId = null
      }
      eventQueue.forEach { (name, value) ->
        sendEventToClarity(name, value)
      }
      eventQueue.clear()
    }
  }
