package com.efina.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.efina.app.MainActivity
import com.efina.app.util.UpdateChecker.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts system notifications when a new app update is available.
 *
 * Creates the notification channel on demand and fires a replaceable notification
 * (fixed ID) so repeated checks don't stack multiple notifications.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    private val channelId = "efina_updates"
    private val notificationId = 1

    /**
     * Creates the notification channel (API 26+). Safe to call on every start.
     */
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Efina Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notify when a new version of Efina is available"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a notification that an update is available. Tapping it opens MainActivity;
     * when the app comes to the foreground the existing UpdateDialog is shown.
     */
    fun notifyUpdate(updateInfo: UpdateInfo) {
        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Efina Update Available")
            .setContentText("Version ${updateInfo.version} is ready to install")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
