package com.hipaduck.timerweb.customtab

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession


class CustomTabActivityHelper(private var connectionCallback: ConnectionCallback) :
    ServiceConnectionCallback {
    private var mCustomTabsSession: CustomTabsSession? = null
    private var mClient: CustomTabsClient? = null
    private var mConnection: CustomTabsServiceConnection? = null

    private val mCustomTabsCallback: CustomTabsCallback = object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            val event = when (navigationEvent) {
                NAVIGATION_ABORTED -> "NAVIGATION_ABORTED"
                NAVIGATION_FAILED -> "NAVIGATION_FAILED"
                NAVIGATION_FINISHED -> "NAVIGATION_FINISHED"
                NAVIGATION_STARTED -> "NAVIGATION_STARTED"
                TAB_SHOWN -> {
                    connectionCallback.onCustomTabShown()
                    "TAB_SHOWN"
                }

                TAB_HIDDEN -> {
                    connectionCallback.onCustomTabHidden()
                    "TAB_HIDDEN"
                }

                else -> navigationEvent.toString()
            }
            Log.d("timer_web", "onNavigationEvent: $event")
        }
    }

    override fun onServiceConnected(client: CustomTabsClient?) {
        mClient = client
        mClient?.warmup(0L)
        connectionCallback.onCustomTabsConnected()
    }

    override fun onServiceDisconnected() {
        mClient = null
        mCustomTabsSession = null
        connectionCallback.onCustomTabsDisconnected()
    }

    /**
     * Creates or retrieves an exiting CustomTabsSession.
     *
     * @return a CustomTabsSession.
     */
    fun getSession(): CustomTabsSession? {
        if (mClient == null) {
            mCustomTabsSession = null
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient?.newSession(mCustomTabsCallback)
        }
        return mCustomTabsSession
    }

    /**
     * Binds the Activity to the Custom Tabs Service.
     * @param activity the activity to be binded to the service.
     */
    fun bindCustomTabsService(activity: Activity?) {
        if (mClient != null) return
        val packageName = activity?.let { CustomTabsHelper.getPackageNameToUse(it) } ?: return
        mConnection = ServiceConnection(this)
        CustomTabsClient.bindCustomTabsService(
            activity!!,
            packageName,
            mConnection as ServiceConnection
        )
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service.
     * @param activity the activity that is connected to the service.
     */
    fun unbindCustomTabsService(activity: Activity) {
        if (mConnection == null) return
        activity.unbindService(mConnection!!)
        mClient = null
        mCustomTabsSession = null
        mConnection = null
    }


    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected.
     */
    interface ConnectionCallback {
        /**
         * Called when the service is connected.
         */
        fun onCustomTabsConnected()

        /**
         * Called when the service is disconnected.
         */
        fun onCustomTabsDisconnected()

        fun onCustomTabHidden()

        fun onCustomTabShown()
    }
}