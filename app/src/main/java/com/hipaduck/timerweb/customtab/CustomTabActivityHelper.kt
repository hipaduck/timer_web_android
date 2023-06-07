package com.hipaduck.timerweb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

class CustomTabActivityHelper(private var connectionCallback: ConnectionCallback): ServiceConnectionCallback {
    private var mCustomTabsSession: CustomTabsSession? = null //GAEGUL: 얘를 넘겨야함
    private var mClient: CustomTabsClient? = null //GAEGUL: 얘로 세션을 생성함
    private var mConnection: CustomTabsServiceConnection? = null //GAEGUL: 얘로 connection 상태를 받음
//    private var mConnectionCallback: ConnectionCallback? = null //GAEGUL: 상태에 대한 콜백
    
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
            mCustomTabsSession = mClient?.newSession(null)
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
        CustomTabsClient.bindCustomTabsService(activity!!, packageName, mConnection as ServiceConnection)
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
    }


    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available.
     */
    interface CustomTabFallback {
        /**
         *
         * @param activity The Activity that wants to open the Uri.
         * @param uri The uri to be opened by the fallback.
         */
        fun openUri(activity: Activity?, uri: Uri?)
    }
}