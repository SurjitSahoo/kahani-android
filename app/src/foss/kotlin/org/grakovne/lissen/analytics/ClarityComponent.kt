package org.grakovne.lissen.analytics

import org.grakovne.lissen.common.RunningComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClarityComponent
  @Inject
  constructor() : RunningComponent {
    override fun onCreate() {}

    fun updateConsent(accepted: Boolean) {}
  }
