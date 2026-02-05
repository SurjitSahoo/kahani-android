package org.grakovne.lissen.analytics

import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClarityTracker
  @Inject
  constructor() {
    fun setUser(userId: String) {
      try {
        Clarity.setCustomUserId(userId)
        Timber.d("Clarity User ID set: $userId")
      } catch (e: Exception) {
        Timber.e(e, "Failed to set Clarity User ID")
      }
    }

    fun trackEvent(eventName: String) {
      try {
        Clarity.sendCustomEvent(eventName)
        Timber.d("Clarity Event tracked: $eventName")
      } catch (e: Exception) {
        Timber.e(e, "Failed to track Clarity event: $eventName")
      }
    }

    fun trackEvent(
      eventName: String,
      value: String,
    ) {
      try {
        // Clarity sendCustomEvent takes a string value for the event name/payload
        Clarity.sendCustomEvent("$eventName: $value")
        Timber.d("Clarity Event tracked: $eventName with value: $value")
      } catch (e: Exception) {
        Timber.e(e, "Failed to track Clarity event: $eventName")
      }
    }
  }
