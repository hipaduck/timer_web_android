package com.hipaduck.timerweb.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
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
            event.getContentIfNotHandled().let { eventPair ->
                when (eventPair?.first) {
                    "notify_on_period" -> shakeTimer()
                    "launch_url" -> launchCustomTab()
                    "present_on_graph" -> {
                        val list = eventPair.second
                        if (list is List<*>) {
                            list.checkItemsAre<Pair<String, Long>>()?.let {
                                showGraph(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showGraph(list: List<Pair<String, Long>>) {
        Log.d("timer_web", "showGraph: list: $list")
        binding.linechartMainWaistTime.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM // x를 아래로
            textSize = 10f
            setDrawGridLines(false) // 배경
            granularity = 10f // x 데이터 표시간격
            axisMinimum = 2f // x 데이터 최소표시
            isGranularityEnabled = true // x 간격 제한하는 기능
            valueFormatter = IndexAxisValueFormatter(list.map { it.first })
        }
        binding.linechartMainWaistTime.apply {
            axisRight.isEnabled = false // y 왼쪽만 사용
            axisLeft.axisMaximum = 60 * 60 * 24f // 하루는 최대 24시간이므로 y 최대값
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
            lineData.addDataSet(set)
            list.forEachIndexed { index, pair ->
                lineData.addEntry(Entry(index.toFloat(), pair.second.toFloat()), 0)
            }
            lineData.notifyDataChanged()
            binding.linechartMainWaistTime.apply {
                notifyDataSetChanged()
                moveViewToX(data.entryCount.toFloat())
                setVisibleXRangeMaximum(7f)
                setPinchZoom(false)
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
        setCircleColor(Color.RED)
        valueTextSize = 10f
        lineWidth = 2f
        circleRadius = 3f
        fillAlpha = 0
        fillColor = Color.DKGRAY
        highLightColor = Color.GREEN
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

    override fun onCustomTabShown() {
        mainViewModel.countTime() // 띄우고 난 뒤부터 카운팅 하기 위함
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

    companion object {
        const val ANIMATION_DELAY_SEC = 2000L
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> List<*>.checkItemsAre() =
    if (all { it is T })
        this as List<T>
    else null