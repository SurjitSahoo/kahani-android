package org.grakovne.lissen.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClarityTracker
  @Inject
  constructor() {
    fun setUser(userId: String) {}

    fun trackEvent(eventName: String) {}

    fun trackEvent(
      eventName: String,
      value: String,
    ) {}
  }
