package com.hipaduck.timerweb.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.text.format.DateUtils
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipaduck.timerweb.Event
import com.hipaduck.timerweb.R
import com.hipaduck.timerweb.SearchUrl
import com.hipaduck.timerweb.data.TimerWebRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val timerWebRepository: TimerWebRepository,
    private val datastore: DataStore<Preferences>,
) : ViewModel() {
    private var timerSec = 0
    private val _timer = MutableLiveData<String>("00:00")
    val timer: MutableLiveData<String>
        get() = _timer

    private val searchUrlList: MutableList<String> = mutableListOf()

    private var jobNotify: Job? = null
    private var jobWriteCurrentTime: Job? = null
    private var jobCount: Job? = null

    private val _actionEvent: MutableLiveData<Event<Pair<String, Any?>>> =
        MutableLiveData(Event("" to null))
    val actionEvent: MutableLiveData<Event<Pair<String, Any?>>>
        get() = _actionEvent

    val inputUrlText: MutableLiveData<String> = MutableLiveData("")

    init {
        presentGraphFromDates()
    }

    fun updateUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            timerWebRepository.putSearchUrl(url)
        }
    }

    fun refreshUrlList() {
        val urlList = arrayListOf<SearchUrl>()

        viewModelScope.launch(Dispatchers.IO) {
            urlList.addAll(timerWebRepository.getSearchUrl())
            searchUrlList.clear()
            searchUrlList.addAll(urlList.map { it.url })
            withContext(Dispatchers.Main) {
                _actionEvent.value = Event("refresh_search_url_list" to searchUrlList)
            }
        }
    }

    fun startWebBrowsing() {
        _actionEvent.value = Event("launch_url" to null)
    }

    fun repeatWork() {
        notifyPeriodically(20_000L)
        writePeriodically(10_000L)
    }

    fun stopRepeatWork() {
        jobNotify?.cancel("notify work stopped")
        jobWriteCurrentTime?.cancel("write work stopped")
    }

    fun applyListTextToCurrentText(idx: Int) {
        Log.d("timer_web", "applyListTextToCurrentText: $idx")
        viewModelScope.launch(Dispatchers.IO) {
            val urlList = timerWebRepository.getSearchUrl()
            withContext(Dispatchers.Main) {
                inputUrlText.value = urlList.map { it.url }[idx]
            }
        }
    }

    fun countTime() {
        jobCount = viewModelScope.launch {
            repeat(Int.MAX_VALUE) {
                Log.d("timer_web", "countTime")
                delay(1000L)
                _timer.value = DateUtils.formatElapsedTime((++timerSec).toLong())
            }
        }
    }

    fun pauseTime() {
        jobCount?.cancel("web browsing stopped")
    }

    private fun playSoundEffect() =
        MediaPlayer.create(application.applicationContext, R.raw.alarm).start()

    private fun notifyPeriodically(periodTime: Long = DEFAULT_PERIOD_TIME) {
        jobNotify = viewModelScope.launch {
            repeat(Int.MAX_VALUE) {
                Log.d("timer_web", "notifyPeriodically")
                delay(periodTime)
                _actionEvent.value = Event("notify_on_period" to null)
                playSoundEffect()
            }
        }
    }

    private fun writePeriodically(periodTime: Long = DEFAULT_PERIOD_TIME) {
        jobWriteCurrentTime = viewModelScope.launch {
            repeat(Int.MAX_VALUE) {
                Log.d("timer_web", "writePeriodically")
                delay(periodTime)
                // 오늘 날짜를 문자열로 만든다
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                Log.d("timer_web", "writePeriodically- today: $today")
                // 현재 저장된 시간을 가져온다
                val storedAccumulatedTime = timerWebRepository.getValueFromDateKey(today)
                // 현재 저장된 시간을 갱신한다(periodTime만큼 흘렀다고 가정)
                timerWebRepository.putValueFromDateKey(
                    today,
//                    5000
                    storedAccumulatedTime.plus(periodTime / 1_000) // 초를 표현하기 위해 1000을 나눔
                )
                // 유효한 날짜 목록을 가져온다
                val storedDates = timerWebRepository.getValidDates()
                Log.d("timer_web", "writePeriodically- storedDates: $storedDates")
                // 만약 유효한 날짜 목록에 오늘이 없다면 오늘을 저장한다
                if (!storedDates.contains(today)) {
                    storedDates.toMutableList().apply {
                        add(today)
                        timerWebRepository.putValidDates(this)
                    }
                    val afterStoredDates = timerWebRepository.getValidDates()
                    Log.d("timer_web", "writePeriodically- after storedDates: $afterStoredDates")
                }
            }
        }
    }

    private fun presentGraphFromDates() {
        viewModelScope.launch(Dispatchers.IO) {
            val storedDates = timerWebRepository.getValidDates()
            val fullDataList = mutableListOf<Pair<String, Long>>()
            for (date in storedDates) {
                if (date.isEmpty()) continue
                val timeValue = timerWebRepository.getValueFromDateKey(date)
                fullDataList.add(date to timeValue)
            }
            Log.d("timer_web", "presentGraphFromDates-fullDataList: $fullDataList")
            withContext(Dispatchers.Main) {
                _actionEvent.value = Event("present_on_graph" to fullDataList)
            }
        }
    }

    // todo 신규 정의/호출해야 하는 함수 목록
    // timer 10초 갱신시마다(매초 보다는 효율적일듯) 현재 타이머의 값 저장하기
    // 앱을 구동시마다 로컬 저장소 값 기준으로 그래프 표현하기

    companion object {
        const val DEFAULT_PERIOD_TIME = 1000L * 60L * 10L // 10분
    }
}