package com.damonjess.doomsdayassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ProjectionDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val resultCode = intent.getIntExtra("result_code", -1)
        val data = intent.getParcelableExtra<Intent>("data")
        if (resultCode != -1 && data != null) {
            val serviceIntent = Intent(context, FloatingButtonService::class.java)
            context.startService(serviceIntent)
        }
    }
}