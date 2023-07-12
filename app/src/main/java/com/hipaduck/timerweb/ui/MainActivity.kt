package com.hipaduck.timerweb.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.hipaduck.timerweb.R
import com.hipaduck.timerweb.common.logd
import com.hipaduck.timerweb.customtab.CustomTabActivityHelper
import com.hipaduck.timerweb.databinding.ActivityMainBinding
import com.hipaduck.timerweb.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CustomTabActivityHelper.ConnectionCallback {
    lateinit var binding: ActivityMainBinding
    private lateinit var mCustomTabActivityHelper: CustomTabActivityHelper

    private val mainViewModel by viewModels<MainViewModel>()

    @SuppressLint("ClickableViewAccessibility")
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

        binding.mainUrlEt.setOnFocusChangeListener { _, isFocused ->
            if (isFocused) {
                binding.vm?.refreshUrlList()
                binding.mainUrlLv.visibility = View.VISIBLE
            } else {
                binding.mainUrlLv.visibility = View.GONE
            }
        }

        binding.mainUrlLv.setOnItemClickListener { v, _, i, _ ->
            logd("onCreate: setOnItemClickListener $i")
            binding.vm?.applyListTextToCurrentText(i)
            v.visibility = View.GONE
        }


        binding.vm?.refreshUrlList()
    }

    private fun initializeBinding() {
        mainViewModel.actionEvent.observe(this) { event ->
            event.getContentIfNotHandled().let { eventPair ->
                when (eventPair?.first) {
                    "notify_on_period" -> shakeTimer()
                    "launch_url" -> launchCustomTab()
                    "present_on_graph" -> {
                        val list = eventPair.second
                        if (list !is List<*>) return@observe
                        list.checkItemsAre<Pair<String, Long>>()?.let { showGraph(it) }
                    }

                    "refresh_search_url_list" -> {
                        val list = eventPair.second
                        if (list !is List<*>) return@observe
                        list.checkItemsAre<String>()?.let {
                            binding.mainUrlLv.adapter =
                                ArrayAdapter(this, android.R.layout.simple_list_item_1, it)
                        }
                    }
                }
            }
        }
    }

    private fun showGraph(list: List<Pair<String, Long>>) {
        logd("showGraph: list: $list")
        binding.linechartMainWaistTime.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM // x를 아래로
            textSize = 10f
            setDrawGridLines(false) // 배경
            granularity = 1f // x 데이터 표시간격
            axisMinimum = 1f // x 데이터 최소표시
            isGranularityEnabled = true // x 간격 제한하는 기능
            valueFormatter = IndexAxisValueFormatter(list.map { it.first })
        }
        binding.linechartMainWaistTime.apply {
            axisRight.isEnabled = false // y 왼쪽만 사용
            axisLeft.axisMaximum = 60 * 60 * 3f // y 최대값: 3시간
            legend.apply {
                textSize = 15f
                verticalAlignment = Legend.LegendVerticalAlignment.TOP // 수직조정. 위로
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER // 수평조절. 가운데
                orientation = Legend.LegendOrientation.HORIZONTAL // 범례와 차트. 수평
                setDrawInside(false) // 차트 바깥으로 표현
            }
        }
        val lineData = LineData()
        binding.linechartMainWaistTime.data = lineData
        lifecycleScope.launch {
            val set = createSetForGraph()
            lineData.apply {
                addDataSet(set)
                list.forEachIndexed { index, pair ->
                    logd("list.forEach: index.toFloat(): ${index.toFloat()},  pair.second.toFloat(): ${pair.second.toFloat()}")
                    addEntry(Entry(index.toFloat(), pair.second.toFloat()), 0)
                }
                notifyDataChanged()
            }
            binding.linechartMainWaistTime.apply {
                notifyDataSetChanged()
                moveViewToX(data.entryCount.toFloat())
                setVisibleXRangeMaximum(7f)
                setPinchZoom(true)
                isDoubleTapToZoomEnabled = false
                description.text = "시간(초)"
                setBackgroundColor(Color.TRANSPARENT)
                description.textSize = 15f
                setExtraOffsets(5f, 10f, 5f, 8f)
            }
        }
    }

    private fun createSetForGraph(): LineDataSet = LineDataSet(null, "시간").apply {
        axisDependency = YAxis.AxisDependency.LEFT
        color = Color.BLUE
        setCircleColor(resources.getColor(R.color.text_color, null))
        valueTextSize = 10f
        lineWidth = 2f
        circleRadius = 3f
        fillAlpha = 0
        fillColor = Color.DKGRAY
        highLightColor = resources.getColor(R.color.highlight_color, null)
        setDrawValues(true)
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
        logd("Connection connected")
    }

    override fun onCustomTabsDisconnected() {
        logd("Connection disconnected")
        mCustomTabActivityHelper.unbindCustomTabsService(this)
    }

    override fun onCustomTabShown() {
        // 띄우고 난 뒤부터 카운팅 하기 위함
        mainViewModel.countTime()
        mainViewModel.repeatWork()
    }

    override fun onCustomTabHidden() {
        mainViewModel.pauseTime()
        mainViewModel.stopRepeatWork()
    }

    private fun searchEtClearFocus() {
        binding.mainUrlEt.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            binding.background.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun launchCustomTab() {
        // Uses the established session to build a PCCT intent.
        val session = mCustomTabActivityHelper.getSession()
        val height = windowManager.currentWindowMetrics.bounds.height()

        var url = binding.mainUrlEt.text.toString()
        if (!url.startsWith("http")) {
            url = "https://$url"
        }
        binding.vm?.updateUrl(url)

        val customTabsIntent: CustomTabsIntent = CustomTabsIntent.Builder(session)
            .setInitialActivityHeightPx(height - getTimerPosition())
            .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(resources.getColor(R.color.background_color, null))
                    .build()
            )
            .build()
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(this, "Unavailable Uri", Toast.LENGTH_SHORT).show()
        }

        binding.vm?.refreshUrlList()
        searchEtClearFocus()
    }

    companion object {
        const val ANIMATION_DELAY_SEC = 2000L
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> List<*>.checkItemsAre() =
    if (all { it is T })
        this as List<T>
    else null