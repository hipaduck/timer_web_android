package com.hipaduck.timerweb.customtab

import android.content.ComponentName
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import java.lang.ref.WeakReference

class CustomTabServiceConnectionWrapper(connectionCallback: CustomTabServiceConnectionCallback) :
    CustomTabsServiceConnection() {
    private val connectionCallbackRef: WeakReference<CustomTabServiceConnectionCallback>

    init {
        connectionCallbackRef = WeakReference(connectionCallback)
    }

    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        connectionCallbackRef.get()?.onServiceConnected(client)
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        connectionCallbackRef.get()?.onServiceDisconnected()
    }
}