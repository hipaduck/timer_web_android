package com.hipaduck.timerweb.common

import android.util.Log

internal const val LOG_TAG = "timer_web"

internal fun Any.logd(message: String) {
    Log.d(LOG_TAG, message)
}