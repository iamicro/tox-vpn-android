package com.toxvpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class ToxVpnService : VpnService(), CoreCallbackHandler {

    private var vpnInterface: android.os.ParcelFileDescriptor? = null
    private var core: CoreController? = null

    private val config = """
    {
      "log": {
        "loglevel": "warning"
      },
      "inbounds": [],
      "outbounds": [
        {
          "tag": "proxy",
          "protocol": "vless",
          "settings": {
            "vnext": [
              {
                "address": "178.105.153.241",
                "port": 443,
                "users": [
                  {
                    "id": "aac775af-932d-4714-bee4-38635ab5ec5c",
                    "encryption": "none"
                  }
                ]
              }
            ]
          },
          "streamSettings": {
            "network": "tcp",
            "security": "reality",
            "realitySettings": {
              "serverName": "amzn.com",
              "fingerprint": "chrome",
              "publicKey": "YfkjRZCFNJrvVGqK_fQASOC7zGRGhQ01s6YB4l3m9js",
              "shortId": "39a1b1",
              "spiderX": "/05104a49a981433"
            }
          }
        }
      ]
    }
    """.trimIndent()

    override fun onCreate() {
        super.onCreate()

        Seq.setContext(applicationContext)

        Libv2ray.initCoreEnv(
            filesDir.absolutePath,
            "tox-vpn"
        )

        createNotificationChannel()

        val notification = Notification.Builder(this, "tox_vpn")
            .setContentTitle("TOX VPN")
            .setContentText("Connecting...")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (core?.isRunning == true) {
            return START_STICKY
        }

        startVpn()

        return START_STICKY
    }

    private fun startVpn() {

        vpnInterface?.close()

        vpnInterface = Builder()
            .setSession("TOX VPN")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .establish()

        val tun = vpnInterface ?: return

        core = Libv2ray.newCoreController(this)

        Thread {
            try {
                core?.startLoop(
                    config,
                    tun.fd
                )
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {

        try {
            core?.stopLoop()
        } catch (_: Exception) {
        }

        core = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }

        vpnInterface = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun startup(): Long {
        return 0L
    }

    override fun shutdown(): Long {
        return 0L
    }

   
    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "tox_vpn",
                "TOX VPN",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
