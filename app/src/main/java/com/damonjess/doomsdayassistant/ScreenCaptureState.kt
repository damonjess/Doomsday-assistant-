package com.damonjess.doomsdayassistant

import android.content.Intent
import android.media.projection.MediaProjection

object ScreenCaptureState {
    var resultCode: Int = -1
    var data: Intent? = null
    var mediaProjection: MediaProjection? = null
}
