package org.grakovne.lissen.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpAnalyticsTracker
  @Inject
  constructor() : AnalyticsTracker {
    override fun trackEvent(
      name: String,
      value: String?,
    ) {
      // Do nothing
    }

    override fun setUser(id: String) {
      // Do nothing
    }

    override fun updateConsent(accepted: Boolean) {
      // Do nothing
    }
  }
