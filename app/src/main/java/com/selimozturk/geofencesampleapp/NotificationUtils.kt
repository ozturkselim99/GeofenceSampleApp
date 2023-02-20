package com.selimozturk.geofencesampleapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat


fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val notificationChannel =
            NotificationChannel(
                "GeofenceChannel",
                "Geofencing",
                NotificationManager.IMPORTANCE_HIGH
            )
        notificationChannel.enableLights(true)
        notificationChannel.enableVibration(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

fun NotificationManager.sendNotification(
    context: Context,
    title: String,
    body: String
) {

    //Opening the Notification
    val contentIntent = Intent(context, MapsActivity::class.java)
    val contentPendingIntent = PendingIntent.getActivity(
        context,
        35,
        contentIntent,
        if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    //Building the notification
    val builder = NotificationCompat.Builder(context, "GeofenceChannel")
        .setStyle(
            NotificationCompat.BigTextStyle()
                .setSummaryText("Summary")
                .setBigContentTitle(title)
                .bigText(body)
        )
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .build()

    this.notify(35, builder)
}
