package org.grakovne.lissen.domain.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.grakovne.lissen.BuildConfig
import org.grakovne.lissen.R
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckerService
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
  ) {
    private val client =
      OkHttpClient
        .Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates() {
      withContext(Dispatchers.IO) {
        try {
          val request =
            Request
              .Builder()
              .url(GITHUB_RELEASES_URL)
              .header("Accept", "application/vnd.github.v3+json")
              .build()

          client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              Timber.e("Update check failed: ${response.code}")
              return@withContext
            }

            val json = response.body?.string() ?: return@withContext
            val releaseNode = JSONObject(json)
            val tagName = releaseNode.optString("tag_name", "")
            val htmlUrl = releaseNode.optString("html_url", "")

            if (tagName.isNotEmpty() && isNewerVersion(tagName, BuildConfig.VERSION_NAME)) {
              showUpdateNotification(tagName, htmlUrl)
            }
          }
        } catch (e: Exception) {
          Timber.e(e, "Failed to check for updates")
        }
      }
    }

    private fun isNewerVersion(
      remoteTag: String,
      localVersion: String,
    ): Boolean {
      val remote = remoteTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
      val local = localVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

      val length = maxOf(remote.size, local.size)
      for (i in 0 until length) {
        val r = remote.getOrElse(i) { 0 }
        val l = local.getOrElse(i) { 0 }
        if (r > l) return true
        if (r < l) return false
      }
      return false
    }

    private fun showUpdateNotification(
      version: String,
      url: String,
    ) {
      val channelId = "app_updates"
      val notificationId = 1001

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.getString(R.string.notification_channel_updates_name)
        val descriptionText = context.getString(R.string.notification_channel_updates_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel =
          NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
          }
        val notificationManager: NotificationManager =
          context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
      }

      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          0,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      val builder =
        NotificationCompat
          .Builder(context, channelId)
          .setSmallIcon(R.drawable.ic_notification_silhouette)
          .setContentTitle(context.getString(R.string.notification_update_available_title, version))
          .setContentText(context.getString(R.string.notification_update_available_body))
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setContentIntent(pendingIntent)
          .setAutoCancel(true)

      try {
        with(NotificationManagerCompat.from(context)) {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
              ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
              notify(notificationId, builder.build())
            }
          } else {
            notify(notificationId, builder.build())
          }
        }
      } catch (e: SecurityException) {
        Timber.e(e, "Permission denied for notifications")
      }
    }

    companion object {
      private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/SurjitSahoo/kahani-android/releases/latest"
    }
  }
