package com.example.youtubemusicdownloader.downloadService

import android.app.Notification
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import com.example.youtubemusicdownloader.R
import com.example.youtubemusicdownloader.musicDownload.MusicDownloadActivity
import java.util.*


class DownloadNotification(val context: Context) {

    private val channelId = "download_progress"
    private var mNotifyManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private var mBuilder = NotificationCompat.Builder(context, channelId)
    private val id = 1
    private var lastUpdateTime: Long = 0
    private val updateInterval = 1000

    init {
        registerChannel()

        val intent = Intent(context, MusicDownloadActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        mBuilder.setContentTitle(context.getString(R.string.download_song))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)


    }

    fun setDescription(description: String){
        mBuilder.setContentText(description)
    }

    fun setProgress(progress: Int){
        if (Date().time - lastUpdateTime < updateInterval && progress != 100){
            return
        }
        if (progress < 100 && notificationIsVisible()) {
            mBuilder.setProgress(100, progress, false)
            mNotifyManager.notify(id, mBuilder.build())
        } else if (progress == 100){
            mBuilder.setProgress(0, 0, false)
                .setContentTitle(context.getString(R.string.download_complete))
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
            mNotifyManager.notify(id, mBuilder.build())
        }
    }

    fun cancelNotification(){
        mNotifyManager.cancel(id)
    }

    fun showNotification(){
        mNotifyManager.notify(id, mBuilder.build())
    }

    private fun registerChannel(){
        val channelTitle = context.getString(R.string.download_notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            var mChannel = mNotifyManager.getNotificationChannel(channelId)
            if (mChannel == null) {
                mChannel = NotificationChannel(channelId, channelTitle, importance)
                mChannel.setSound(null, null)
                mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                mNotifyManager.createNotificationChannel(mChannel)
            }
        }
    }

    private fun notificationIsVisible(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNotifyManager.activeNotifications.map { it.id }.contains(id)
        } else {
            true
        }
    }
}