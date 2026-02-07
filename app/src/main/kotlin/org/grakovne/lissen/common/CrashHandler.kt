package org.grakovne.lissen.common

import android.content.Context
import android.os.Looper
import android.widget.Toast
import org.grakovne.lissen.R

class CrashHandler(
  private val context: Context,
  private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(
    thread: Thread,
    ex: Throwable,
  ) {
    try {
      object : Thread() {
        override fun run() {
          Looper.prepare()
          Toast.makeText(context, R.string.app_crach_toast, Toast.LENGTH_LONG).show()
          Looper.loop()
        }
      }.start()

      Thread.sleep(TOAST_TIMEOUT)
    } catch (e: Exception) {
      // Ignore errors in the crash handler itself to avoid infinite loops
    } finally {
      defaultHandler?.uncaughtException(thread, ex)
    }
  }

  companion object {
    private const val TOAST_TIMEOUT = 1000L
  }
}
