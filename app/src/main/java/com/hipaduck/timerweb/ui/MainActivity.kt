package com.hipaduck.timerweb.ui

import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hipaduck.timerweb.CustomTabActivityHelper
import com.hipaduck.timerweb.R
import com.hipaduck.timerweb.databinding.ActivityMainBinding
import com.hipaduck.timerweb.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CustomTabActivityHelper.ConnectionCallback {
    private lateinit var mainViewModel: MainViewModel
    lateinit var binding: ActivityMainBinding
    private var mCustomTabActivityHelper: CustomTabActivityHelper? = null
    private val defaultVisibleRect = Rect()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mCustomTabActivityHelper = CustomTabActivityHelper(this)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        binding.vm = mainViewModel
        binding.lifecycleOwner = this

        binding.mainTimerTv.setOnClickListener {
            binding.monitoringView.getGlobalVisibleRect(defaultVisibleRect)

            // Uses the established session to build a PCCT intent.
            val session = mCustomTabActivityHelper!!.getSession()
            val height = windowManager.currentWindowMetrics.bounds.height()
            Log.d("TAG", "height: $height")
            Log.d("TAG", "request: ${height - getTimerPosition()}")
            val customTabsIntent: CustomTabsIntent = CustomTabsIntent.Builder(session)
                .setInitialActivityHeightPx(height - getTimerPosition())
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse("https://www.naver.com"))

            lifecycleScope.launch {
                while (true) {
                    val rect: Rect = Rect()
                    binding.monitoringView.getGlobalVisibleRect(rect)

                    Log.d("GAEUGL", "new rect height : ${rect.height()}")
                    Log.d("GAEUGL", "default rect height : ${defaultVisibleRect.height()}")
                    delay(300)
                }
            }
        }
    }

    private fun getTimerPosition(): Int {
        val position: IntArray = intArrayOf(0, 0)
        binding.mainTimerTv.getLocationOnScreen(position)
        Log.d("TAG", "getTimerPosition: ${position[1]}")
        return position[1] + binding.mainTimerTv.height
    }

    override fun onResume() {
        super.onResume()
        Log.d("LIFE", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("LIFE", "onPause")
    }

    override fun onStart() {
        super.onStart()
        mCustomTabActivityHelper?.bindCustomTabsService(this)
    }

    override fun onStop() {
        super.onStop()
        Log.d("LIFE", "onStop")
        mCustomTabActivityHelper?.unbindCustomTabsService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LIFE", "onDestroy")
        mCustomTabActivityHelper?.unbindCustomTabsService(this)
    }

    override fun onCustomTabsConnected() {}

    override fun onCustomTabsDisconnected() {
        mCustomTabActivityHelper?.unbindCustomTabsService(this)
    }
}