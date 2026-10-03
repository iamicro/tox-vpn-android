package com.toxvpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.util.Log
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
        "loglevel": "debug"
      },

      "inbounds": [
        {
          "tag": "tun-in",
          "port": 0,
          "protocol": "tun",
          "settings": {
            "name": "tun0",
            "MTU": 1500
          }
        }
      ],

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
      ],

      "routing": {
        "domainStrategy": "AsIs",
        "rules": [
          {
            "type": "field",
            "inboundTag": [
              "tun-in"
            ],
            "outboundTag": "proxy"
          }
        ]
      }
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
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
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

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }

        vpnInterface = Builder()
            .setSession("TOX VPN")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .establish()

        val tun = vpnInterface

        if (tun == null) {
            Log.e("TOX_XRAY", "Failed to establish VPN interface")
            stopSelf()
            return
        }

        core = Libv2ray.newCoreController(this)

        Thread {

            try {

                Log.d(
                    "TOX_XRAY",
                    "Starting Xray with TUN fd=${tun.fd}"
                )

                core?.startLoop(
                    config,
                    tun.fd
                )

                Log.d(
                    "TOX_XRAY",
                    "Xray startLoop returned successfully"
                )

            } catch (e: Exception) {
                Log.e(
                    "TOX_XRAY",
                    "Xray failed to start",
                    e
                )

                stopSelf()
            }

        }.start()
    }

    override fun onDestroy() {

        Log.d("TOX_XRAY", "Stopping VPN")

        try {
            core?.stopLoop()
        } catch (e: Exception) {
            Log.e("TOX_XRAY", "Core stop error", e)
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
        Log.d("TOX_XRAY", "Xray started")
        return 0L
    }

    override fun shutdown(): Long {
        Log.d("TOX_XRAY", "Xray stopped")
        return 0L
    }

    override fun onEmitStatus(
        l: Long,
        s: String?
    ): Long {

        Log.d(
            "TOX_XRAY",
            "Status: $s"
        )

        return 0L
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "tox_vpn",
                "TOX VPN",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }
}
