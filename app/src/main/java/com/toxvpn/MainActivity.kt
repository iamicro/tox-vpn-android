package com.toxvpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "TOX VPN v0.1\nDisconnected"
            textSize = 20f
            setPadding(40, 60, 40, 30)
        }

        val connect = Button(this).apply {
            text = "CONNECT"
        }

        connect.setOnClickListener {
            val intent = VpnService.prepare(this)

            if (intent != null) {
                startActivityForResult(intent, 100)
            } else {
                startVpn()
            }
        }

        val disconnect = Button(this).apply {
            text = "DISCONNECT"
        }

        disconnect.setOnClickListener {
            stopService(
                Intent(this, ToxVpnService::class.java)
            )

            status.text = "TOX VPN v0.1\nDisconnected"
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(status)
                addView(connect)
                addView(disconnect)
            }
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 100 && resultCode == RESULT_OK) {
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, ToxVpnService::class.java)

        startForegroundService(intent)

        status.text = "TOX VPN v0.1\nConnecting..."
    }
}
