package com.damonjess.doomsdayassistant

import android.content.Intent
import android.media.projection.MediaProjection

object ScreenCaptureState {
    var resultCode: Int = 0 // Changed from -1 to 0 (RESULT_CANCELED)
    var data: Intent? = null
    var mediaProjection: MediaProjection? = null
}
