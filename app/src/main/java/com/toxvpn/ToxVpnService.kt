package com.toxvpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder

class ToxVpnService : VpnService() {

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "tox_vpn",
                "TOX VPN",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        startForeground(
            1,
            Notification.Builder(this, "tox_vpn")
                .setContentTitle("TOX VPN")
                .setContentText("Connecting...")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}
