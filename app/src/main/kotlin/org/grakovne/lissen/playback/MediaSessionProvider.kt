package org.grakovne.lissen.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import org.grakovne.lissen.R
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import org.grakovne.lissen.ui.activity.AppActivity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionProvider
  @OptIn(UnstableApi::class)
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val exoPlayer: ExoPlayer,
    private val preferences: LissenSharedPreferences,
  ) {
    @OptIn(UnstableApi::class)
    fun provideMediaSession(): MediaSession {
      val sessionActivityPendingIntent =
        PendingIntent.getActivity(
          context,
          0,
          Intent(context, AppActivity::class.java),
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

      return MediaSession
        .Builder(context, exoPlayer)
        .setCallback(
          object : MediaSession.Callback {
            override fun onMediaButtonEvent(
              session: MediaSession,
              controllerInfo: MediaSession.ControllerInfo,
              intent: Intent,
            ): Boolean {
              Timber.d("Executing media button event from: $controllerInfo")

              val keyEvent =
                intent
                  .getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                  ?: return super.onMediaButtonEvent(session, controllerInfo, intent)

              Timber.d("Got media key event: $keyEvent")

              if (keyEvent.action != KeyEvent.ACTION_DOWN) {
                return super.onMediaButtonEvent(session, controllerInfo, intent)
              }

              when (keyEvent.keyCode) {
                KEYCODE_MEDIA_NEXT -> {
                  mediaRepository.forward()
                  return true
                }

                KEYCODE_MEDIA_PREVIOUS -> {
                  mediaRepository.rewind()
                  return true
                }

                else -> return super.onMediaButtonEvent(session, controllerInfo, intent)
              }
            }

            @OptIn(UnstableApi::class)
            override fun onConnect(
              session: MediaSession,
              controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
              val rewindCommand = SessionCommand(REWIND_COMMAND, Bundle.EMPTY)
              val forwardCommand = SessionCommand(FORWARD_COMMAND, Bundle.EMPTY)
              val skipPreviousCommand = SessionCommand(SKIP_PREVIOUS_COMMAND, Bundle.EMPTY)
              val skipNextCommand = SessionCommand(SKIP_NEXT_COMMAND, Bundle.EMPTY)

              val showNavButtons = preferences.getShowPlayerNavButtons()
              val seekTime = preferences.getSeekTime()

              val sessionCommandsBuilder =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                  .buildUpon()
                  .add(rewindCommand)
                  .add(forwardCommand)
                  .add(skipPreviousCommand)
                  .add(skipNextCommand)

              if (!showNavButtons) {
                // Do nothing for session commands, as skip is a player command
              }

              val sessionCommands = sessionCommandsBuilder.build()

              val playerCommandsBuilder =
                Player.Commands.EMPTY
                  .buildUpon()
                  .add(Player.COMMAND_GET_AUDIO_ATTRIBUTES)
                  .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                  .add(Player.COMMAND_GET_DEVICE_VOLUME)
                  .add(Player.COMMAND_GET_METADATA)
                  .add(Player.COMMAND_GET_TEXT)
                  .add(Player.COMMAND_GET_TIMELINE)
                  .add(Player.COMMAND_GET_TRACKS)
                  .add(Player.COMMAND_GET_VOLUME)
                  .add(Player.COMMAND_PLAY_PAUSE)
                  .add(Player.COMMAND_RELEASE)
                  .add(Player.COMMAND_SEEK_BACK)
                  .add(Player.COMMAND_SEEK_FORWARD)
                  .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                  .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                  .add(Player.COMMAND_SET_DEVICE_VOLUME)
                  .add(Player.COMMAND_SET_MEDIA_ITEM)
                  .add(Player.COMMAND_SET_REPEAT_MODE)
                  .add(Player.COMMAND_SET_SHUFFLE_MODE)
                  .add(Player.COMMAND_SET_SPEED_AND_PITCH)
                  .add(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)
                  .add(Player.COMMAND_SET_VOLUME)
                  .add(Player.COMMAND_STOP)

              if (showNavButtons) {
                // We provide these via custom buttons in the customLayout to ensure
                // the ordering and icons are exactly as the user expects.
                // Adding them here can cause the OS to inject duplicate standard buttons.
              }

              val rewindButton =
                CommandButton
                  .Builder(CommandButton.ICON_UNDEFINED)
                  .setSessionCommand(rewindCommand)
                  .setDisplayName("Rewind")
                  .setIconResId(resolveSeekIcon(seekTime.rewind.seconds, false))
                  .setEnabled(true)
                  .build()

              val forwardButton =
                CommandButton
                  .Builder(CommandButton.ICON_UNDEFINED)
                  .setSessionCommand(forwardCommand)
                  .setDisplayName("Forward")
                  .setIconResId(resolveSeekIcon(seekTime.forward.seconds, true))
                  .setEnabled(true)
                  .build()

              val skipPreviousButton =
                CommandButton
                  .Builder(CommandButton.ICON_UNDEFINED)
                  .setSessionCommand(skipPreviousCommand)
                  .setDisplayName("Previous")
                  .setIconResId(R.drawable.ic_n_nav_skip_previous)
                  .setEnabled(true)
                  .build()

              val skipNextButton =
                CommandButton
                  .Builder(CommandButton.ICON_UNDEFINED)
                  .setSessionCommand(skipNextCommand)
                  .setDisplayName("Next")
                  .setIconResId(R.drawable.ic_n_nav_skip_next)
                  .setEnabled(true)
                  .build()

              val customLayout =
                if (showNavButtons) {
                  listOf(skipPreviousButton, skipNextButton, rewindButton, forwardButton)
                } else {
                  listOf(rewindButton, forwardButton)
                }

              return MediaSession
                .ConnectionResult
                .AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommandsBuilder.build())
                .setCustomLayout(customLayout)
                .build()
            }

            override fun onCustomCommand(
              session: MediaSession,
              controller: MediaSession.ControllerInfo,
              customCommand: SessionCommand,
              args: Bundle,
            ): ListenableFuture<SessionResult> {
              Timber.d("Executing: ${customCommand.customAction}")

              when (customCommand.customAction) {
                REWIND_COMMAND -> {
                  val seekTime = preferences.getSeekTime()
                  val currentPosition = exoPlayer.currentPosition
                  val targetPosition = currentPosition - (seekTime.rewind.seconds * 1000)
                  exoPlayer.seekTo(targetPosition.coerceAtLeast(0))
                }

                FORWARD_COMMAND -> {
                  val seekTime = preferences.getSeekTime()
                  val currentPosition = exoPlayer.currentPosition
                  val targetPosition = currentPosition + (seekTime.forward.seconds * 1000)
                  exoPlayer.seekTo(targetPosition)
                }

                SKIP_PREVIOUS_COMMAND -> {
                  mediaRepository.previousTrack()
                }

                SKIP_NEXT_COMMAND -> {
                  mediaRepository.nextTrack()
                }
              }
              return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
          },
        ).setSessionActivity(sessionActivityPendingIntent)
        .build()
    }

    @OptIn(UnstableApi::class)
    fun updateSessionCommands(session: MediaSession) {
      val showNavButtons = preferences.getShowPlayerNavButtons()
      val seekTime = preferences.getSeekTime()

      val rewindCommand = SessionCommand(REWIND_COMMAND, Bundle.EMPTY)
      val forwardCommand = SessionCommand(FORWARD_COMMAND, Bundle.EMPTY)
      val skipPreviousCommand = SessionCommand(SKIP_PREVIOUS_COMMAND, Bundle.EMPTY)
      val skipNextCommand = SessionCommand(SKIP_NEXT_COMMAND, Bundle.EMPTY)

      val sessionCommandsBuilder =
        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
          .buildUpon()
          .add(rewindCommand)
          .add(forwardCommand)
          .add(skipPreviousCommand)
          .add(skipNextCommand)

      if (!showNavButtons) {
        // Do nothing for session commands
      }

      val sessionCommands = sessionCommandsBuilder.build()

      val playerCommandsBuilder =
        Player.Commands.EMPTY
          .buildUpon()
          .add(Player.COMMAND_GET_AUDIO_ATTRIBUTES)
          .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
          .add(Player.COMMAND_GET_DEVICE_VOLUME)
          .add(Player.COMMAND_GET_METADATA)
          .add(Player.COMMAND_GET_TEXT)
          .add(Player.COMMAND_GET_TIMELINE)
          .add(Player.COMMAND_GET_TRACKS)
          .add(Player.COMMAND_GET_VOLUME)
          .add(Player.COMMAND_PLAY_PAUSE)
          .add(Player.COMMAND_RELEASE)
          .add(Player.COMMAND_SEEK_BACK)
          .add(Player.COMMAND_SEEK_FORWARD)
          .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
          .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
          .add(Player.COMMAND_SET_DEVICE_VOLUME)
          .add(Player.COMMAND_SET_MEDIA_ITEM)
          .add(Player.COMMAND_SET_REPEAT_MODE)
          .add(Player.COMMAND_SET_SHUFFLE_MODE)
          .add(Player.COMMAND_SET_SPEED_AND_PITCH)
          .add(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)
          .add(Player.COMMAND_SET_VOLUME)
          .add(Player.COMMAND_STOP)

      if (showNavButtons) {
        // Provided via customLayout
      }

      val playerCommands = playerCommandsBuilder.build()

      val rewindButton =
        CommandButton
          .Builder(CommandButton.ICON_UNDEFINED)
          .setSessionCommand(rewindCommand)
          .setDisplayName("Rewind")
          .setIconResId(resolveSeekIcon(seekTime.rewind.seconds, false))
          .setEnabled(true)
          .build()

      val forwardButton =
        CommandButton
          .Builder(CommandButton.ICON_UNDEFINED)
          .setSessionCommand(forwardCommand)
          .setDisplayName("Forward")
          .setIconResId(resolveSeekIcon(seekTime.forward.seconds, true))
          .setEnabled(true)
          .build()

      val skipPreviousButton =
        CommandButton
          .Builder(CommandButton.ICON_UNDEFINED)
          .setSessionCommand(skipPreviousCommand)
          .setDisplayName("Previous")
          .setIconResId(R.drawable.ic_n_nav_skip_previous)
          .setEnabled(true)
          .build()

      val skipNextButton =
        CommandButton
          .Builder(CommandButton.ICON_UNDEFINED)
          .setSessionCommand(skipNextCommand)
          .setDisplayName("Next")
          .setIconResId(R.drawable.ic_n_nav_skip_next)
          .setEnabled(true)
          .build()

      val customLayout =
        if (showNavButtons) {
          listOf(skipPreviousButton, skipNextButton, rewindButton, forwardButton)
        } else {
          listOf(rewindButton, forwardButton)
        }

      session.connectedControllers.forEach { controller ->
        session.setAvailableCommands(controller, sessionCommands, playerCommands)
        session.setCustomLayout(controller, customLayout)
      }
    }

    companion object {
      private const val REWIND_COMMAND = "notification_rewind"
      private const val FORWARD_COMMAND = "notification_forward"
      private const val SKIP_PREVIOUS_COMMAND = "notification_skip_previous"
      private const val SKIP_NEXT_COMMAND = "notification_skip_next"

      private fun provideRewindCommand() = CommandButton.ICON_SKIP_BACK

      private fun provideForwardCommand() = CommandButton.ICON_SKIP_FORWARD

      private fun resolveSeekIcon(
        seconds: Int,
        isForward: Boolean,
      ): Int =
        if (isForward) {
          when (seconds) {
            5 -> R.drawable.ic_notification_forward_5
            10 -> R.drawable.ic_notification_forward_10
            15 -> R.drawable.ic_notification_forward_15
            30 -> R.drawable.ic_notification_forward_30
            60 -> R.drawable.ic_notification_forward_60
            else -> R.drawable.ic_notification_forward_30
          }
        } else {
          when (seconds) {
            5 -> R.drawable.ic_notification_rewind_5
            10 -> R.drawable.ic_notification_rewind_10
            15 -> R.drawable.ic_notification_rewind_15
            30 -> R.drawable.ic_notification_rewind_30
            60 -> R.drawable.ic_notification_rewind_60
            else -> R.drawable.ic_notification_rewind_10
          }
        }
    }
  }
