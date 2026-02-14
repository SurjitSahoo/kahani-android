package org.grakovne.lissen.analytics

interface AnalyticsTracker {
  fun trackEvent(
    name: String,
    value: String? = null,
  )

  fun setUser(id: String)

  fun updateConsent(accepted: Boolean)
}
