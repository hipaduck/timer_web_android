package com.hipaduck.timerweb.customtab

import androidx.browser.customtabs.CustomTabsClient

interface CustomTabServiceConnectionCallback {
    fun onServiceConnected(client: CustomTabsClient?)

    fun onServiceDisconnected()
}