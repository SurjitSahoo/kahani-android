package org.grakovne.lissen.common

import android.content.Context
import timber.log.Timber

class CrashHandler(
  private val context: Context,
  private val crashReporter: CrashReporter,
  private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(
    thread: Thread,
    throwable: Throwable,
  ) {
    Timber.e(throwable, "Uncaught exception in thread ${thread.name}")
    crashReporter.recordException(throwable)
    defaultHandler?.uncaughtException(thread, throwable)
  }
}
