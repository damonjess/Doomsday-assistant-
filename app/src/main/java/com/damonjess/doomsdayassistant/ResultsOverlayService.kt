package com.damonjess.doomsdayassistant

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.tabs.TabLayout

class ResultsOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    
    companion object {
        var currentResult: AnalysisResult? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (currentResult == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.results_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(overlayView, params)
        setupUI()

        return START_NOT_STICKY
    }

    private fun setupUI() {
        val result = currentResult ?: return
        
        overlayView.findViewById<TextView>(R.id.results_title).text = "⚔️ ${result.heroName}"
        overlayView.findViewById<ProgressBar>(R.id.priority_score_bar).progress = result.priorityScore.toInt()
        overlayView.findViewById<TextView>(R.id.priority_score_text).text = "Priority: ${result.priorityScore.toInt()}/100"
        
        val recText = overlayView.findViewById<TextView>(R.id.recommendations_text)
        recText.text = result.recommendations.joinToString("\n• ", "• ")
        
        val arenaText = overlayView.findViewById<TextView>(R.id.arena_text)
        arenaText.text = result.arenaPairs.joinToString("\n• ", "• ")

        val tabLayout = overlayView.findViewById<TabLayout>(R.id.tab_layout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    recText.visibility = View.VISIBLE
                    arenaText.visibility = View.GONE
                } else {
                    recText.visibility = View.GONE
                    arenaText.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        overlayView.findViewById<Button>(R.id.close_button).setOnClickListener {
            stopSelf()
        }

        // Swipe up to dismiss could be added here
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
    }
}
