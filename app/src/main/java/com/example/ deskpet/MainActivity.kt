package com.example.deskpet

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 200, 60, 60)
        }
        val start = Button(this).apply {
            text = "启动桌宠"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "请允许“显示在其他应用上层”，返回后再点启动", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startForegroundService(Intent(this@MainActivity, PetService::class.java))
                    Toast.makeText(this@MainActivity, "桌宠已启动", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val stop = Button(this).apply {
            text = "停止桌宠"
            setOnClickListener {
                stopService(Intent(this@MainActivity, PetService::class.java))
                Toast.makeText(this@MainActivity, "已停止", Toast.LENGTH_SHORT).show()
            }
        }
        box.addView(start)
        box.addView(stop)
        setContentView(box)
    }
}

class PetService : Service() {
    private lateinit var wm: WindowManager
    private var pet: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotice()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (pet == null) addPet()
        return START_STICKY
    }

    private fun startForegroundNotice() {
        val ch = "pet"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(ch, "桌宠", NotificationManager.IMPORTANCE_LOW))
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, ch)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("桌宠运行中")
            .setContentText("拖动小猫，点一下有反应")
            .setContentIntent(pi)
            .build()
        startForeground(1, n)
    }

    private fun addPet() {
        val tv = TextView(this).apply {
            text = "🐱"
            textSize = 48f
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.TRANSPARENT)
        }
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120
            y = 300
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        tv.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    wm.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        Toast.makeText(this, listOf("喵～", "干嘛", "摸摸我", "饿了", "困了", "陪你").random(), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        wm.addView(tv, params)
        pet = tv
    }

    override fun onDestroy() {
        pet?.let { wm.removeView(it) }
        pet = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
