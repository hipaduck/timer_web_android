package com.hipaduck.timerweb.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.hipaduck.timerweb.R
import com.hipaduck.timerweb.customtab.CustomTabActivityHelper
import com.hipaduck.timerweb.databinding.ActivityMainBinding
import com.hipaduck.timerweb.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.MalformedURLException


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CustomTabActivityHelper.ConnectionCallback {
    lateinit var binding: ActivityMainBinding
    private lateinit var mCustomTabActivityHelper: CustomTabActivityHelper
    private val list: MutableList<String> = mutableListOf()
    private val mainViewModel by viewModels<MainViewModel>()

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.vm = mainViewModel
        binding.lifecycleOwner = this

        initializeBinding()

        binding.background.setOnTouchListener { _, _ ->
            searchEtClearFocus()
            true
        }

        binding.mainSearchIv.setOnClickListener {
            // Uses the established session to build a PCCT intent.
            val session = mCustomTabActivityHelper.getSession()
            val height = windowManager.currentWindowMetrics.bounds.height()

            val url = binding.mainUrlEt.text.toString()
            binding.vm?.updateUrl(url)

            val customTabsIntent: CustomTabsIntent = CustomTabsIntent.Builder(session)
                .setInitialActivityHeightPx(height - getTimerPosition())
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build()
            try {
                customTabsIntent.launchUrl(this, Uri.parse(url))
            } catch (e: MalformedURLException) {
                Toast.makeText(this, "Unavailable Uri", Toast.LENGTH_SHORT).show()
            }

            updateUrlListView()
            searchEtClearFocus()
        }

        binding.mainUrlEt.setOnFocusChangeListener { _, isFocused ->
            if (isFocused) {
                updateUrlListView()
                binding.mainUrlLv.visibility = View.VISIBLE
            } else {
                binding.mainUrlLv.visibility = View.GONE
            }
        }

        binding.mainUrlLv.setOnItemClickListener { _, _, i, _ ->
            updateUrlListView()
            list.getOrNull(i)?.let {
                binding.mainUrlEt.setText(it)
            }
            binding.mainUrlLv.visibility = View.GONE
            searchEtClearFocus()
        }
        updateUrlListView()
    }

    private fun initializeBinding() {
        mainViewModel.actionEvent.observe(this) { event ->
            event.getContentIfNotHandled().let { eventName ->
                when (eventName) {
                    "notify_on_period" -> shakeTimer()
                }
            }
        }
    }

    private fun shakeTimer() {
        val anim: Animation = AnimationUtils.loadAnimation(this, R.anim.scale)
        anim.reset()
        binding.mainTimerTv.apply { //animation flow (red -> scale change --2000ms--> original color)
            background = AppCompatResources.getDrawable(
                applicationContext,
                R.drawable.background_square_alert
            )
            setTextColor(Color.RED)
            clearAnimation()
            startAnimation(anim)
            lifecycleScope.launch {
                delay(ANIMATION_DELAY_SEC)
                setTextColor(applicationContext.getColor(R.color.text_color))
                background = AppCompatResources.getDrawable(
                    applicationContext,
                    R.drawable.background_square
                )
                clearAnimation()
            }
        }
    }

    private fun updateUrlListView() {
        list.clear()
        list.addAll(binding.vm!!.getUrlList().map { it.url })
        binding.mainUrlLv.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            list
        )
    }

    private fun getTimerPosition(): Int {
        val position: IntArray = intArrayOf(0, 0)
        binding.mainTimerTv.getLocationOnScreen(position)
        return position[1] + binding.mainTimerTv.height
    }

    override fun onStart() {
        super.onStart()
        mCustomTabActivityHelper = CustomTabActivityHelper(this)
        mCustomTabActivityHelper.bindCustomTabsService(this)
    }

    override fun onStop() {
        super.onStop()
        mCustomTabActivityHelper.unbindCustomTabsService(this)
    }

    override fun onCustomTabsConnected() {
        Log.d("Connection", "connected")
    }

    override fun onCustomTabsDisconnected() {
        Log.d("Connection", "disconnected")
        mCustomTabActivityHelper.unbindCustomTabsService(this)
    }

    private fun searchEtClearFocus() {
        binding.mainUrlEt.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            binding.background.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    companion object {
        const val ANIMATION_DELAY_SEC = 2000L
    }
}