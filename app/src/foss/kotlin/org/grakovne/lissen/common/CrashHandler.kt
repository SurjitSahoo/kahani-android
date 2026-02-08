package org.grakovne.lissen.common

import android.content.Context
import android.os.Looper
import android.widget.Toast
import org.grakovne.lissen.R

class CrashHandler(
  context: Context,
  private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
  private val context = context.applicationContext

  override fun uncaughtException(
    thread: Thread,
    ex: Throwable,
  ) {
    try {
      object : Thread() {
        override fun run() {
          Looper.prepare()
          Toast.makeText(context, R.string.app_crash_toast, Toast.LENGTH_LONG).show()

          // Schedule the looper to quit so the thread doesn't block indefinitely
          android.os.Handler(Looper.myLooper()!!).postDelayed(
            { Looper.myLooper()?.quit() },
            TOAST_TIMEOUT + 500,
          )

          Looper.loop()
        }
      }.start()

      Thread.sleep(TOAST_TIMEOUT)
    } catch (e: Exception) {
      // Ignore errors in the crash handler itself
    } finally {
      defaultHandler?.uncaughtException(thread, ex)
    }
  }

  companion object {
    private const val TOAST_TIMEOUT = 1000L
  }
}
