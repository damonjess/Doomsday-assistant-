package com.damonjess.doomsdayassistant

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import com.google.android.material.tabs.TabLayout

class ResultsOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay(intent)
        return START_NOT_STICKY
    }

    private fun showOverlay(intent: Intent?) {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }

        val view = LayoutInflater.from(this).inflate(R.layout.results_overlay, null)
        overlayView = view

        val title = view.findViewById<TextView>(R.id.results_title)
        val scoreBar = view.findViewById<ProgressBar>(R.id.priority_score_bar)
        val scoreText = view.findViewById<TextView>(R.id.priority_score_text)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val recommendationsText = view.findViewById<TextView>(R.id.recommendations_text)
        val arenaText = view.findViewById<TextView>(R.id.arena_text)
        val closeButton = view.findViewById<Button>(R.id.close_button)

        val screenTitle = intent?.getStringExtra("title") ?: "Analysis"
        val priorityScore = intent?.getIntExtra("priority_score", 50) ?: 50
        val headers = intent?.getStringArrayListExtra("sections_headers") ?: arrayListOf()
        val items = intent?.getStringArrayListExtra("sections_items") ?: arrayListOf()

        title.text = screenTitle
        scoreBar.progress = priorityScore
        scoreText.text = "Priority: $priorityScore/100"
        scoreText.setTextColor(
            when {
                priorityScore >= 80 -> android.graphics.Color.parseColor("#ff4444")
                priorityScore >= 50 -> android.graphics.Color.parseColor("#ffaa00")
                else -> android.graphics.Color.parseColor("#4CAF50")
            }
        )

        val allText = StringBuilder()
        for (i in headers.indices) {
            allText.appendLine("═══ ${headers[i]} ═══")
            allText.appendLine(items.getOrElse(i) { "" })
            allText.appendLine()
        }

        recommendationsText.text = allText.toString().trim()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        recommendationsText.visibility = View.VISIBLE
                        arenaText.visibility = View.GONE
                    }
                    1 -> {
                        recommendationsText.visibility = View.GONE
                        arenaText.visibility = View.VISIBLE
                        if (arenaText.text.isEmpty()) {
                            arenaText.text = "🏟️ Arena data available in the Arena of Doom tab.\n\nTap the floating button → 'Open Arena of Doom' to build teams."
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        closeButton.setOnClickListener {
            stopSelf()
        }

        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val y = event.y
                if (y < view.height * 0.2f) {
                    stopSelf()
                }
            }
            true
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
