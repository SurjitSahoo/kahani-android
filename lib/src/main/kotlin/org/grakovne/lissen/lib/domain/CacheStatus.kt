package org.grakovne.lissen.lib.domain

import androidx.annotation.Keep

@Keep
sealed class CacheStatus {
  data object Idle : CacheStatus()
  data object Queued : CacheStatus()
  data object Caching : CacheStatus()
  data object Completed : CacheStatus()

  data object Error : CacheStatus()
}
