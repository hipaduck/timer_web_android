package com.hipaduck.timerweb.viewmodel

import android.text.format.DateUtils
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipaduck.timerweb.Event
import com.hipaduck.timerweb.data.TimerWebRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val timerWebRepository: TimerWebRepository,
    private val datastore: DataStore<Preferences>,
) : ViewModel() {
    private var timerSec = 0
    private val _timer = MutableLiveData<String>()
    private val _shouldApplyAnimation: MutableLiveData<Boolean> =
        MutableLiveData(false)
    val shouldApplyAnimation: MutableLiveData<Boolean>
        get() = _shouldApplyAnimation

    val timer: MutableLiveData<String>
        get() = _timer

    private var jobNotify: Job? = null
    private var jobCount: Job? = null

    private val _actionEvent: MutableLiveData<Event<String>> = MutableLiveData(Event(""))
    val actionEvent: MutableLiveData<Event<String>>
        get() = _actionEvent

    fun updateUrl(url: String) {
        viewModelScope.launch {
            val urlJsonArrayStr =
                datastore.data.map {
                    it[stringPreferencesKey(
                        URL_SEARCH_HISTORY_PREF_KEY
                    )]
                }.first()

            var urlJsonArray = JSONArray()
            var jsonObject = JSONObject()
                .put(URL_SEARCH_HISTORY_COUNT_KEY, 0)
                .put(URL_SEARCH_HISTORY_TIME_KEY, System.currentTimeMillis())
                .put(URL_SEARCH_HISTORY_URL_KEY, url)
            var foundIndex: Int? = null
            if (!urlJsonArrayStr.isNullOrEmpty()) {
                urlJsonArray = JSONArray(urlJsonArrayStr)
                (0 until urlJsonArray.length()).forEach { i ->
                    Log.d(
                        "GAEGUL",
                        "updateUrl: $i : ${urlJsonArray.length()} / ${urlJsonArray.optJSONObject(i)}"
                    )
                    val obj = urlJsonArray.optJSONObject(i)
                    if (obj.optString(URL_SEARCH_HISTORY_URL_KEY).equals(url)) {
                        jsonObject = JSONObject()
                            .put(
                                URL_SEARCH_HISTORY_COUNT_KEY,
                                obj.optInt(URL_SEARCH_HISTORY_COUNT_KEY) + 1
                            )
                            .put(URL_SEARCH_HISTORY_TIME_KEY, System.currentTimeMillis())
                            .put(URL_SEARCH_HISTORY_URL_KEY, url)
                        foundIndex = i
                    }
                }
            }
            foundIndex?.let {
                urlJsonArray.remove(it)
            }
            urlJsonArray.put(jsonObject)
            datastore.edit {
                it[stringPreferencesKey(
                    URL_SEARCH_HISTORY_PREF_KEY
                )] = urlJsonArray.toString()
            }
        }
    }

    fun getUrlList(): ArrayList<UrlData> {
        val urlList = arrayListOf<UrlData>()

        viewModelScope.launch {
            val urlJsonArrayStr =
                datastore.data.map {
                    it[stringPreferencesKey(
                        URL_SEARCH_HISTORY_PREF_KEY
                    )]
                }.first()

            if (urlJsonArrayStr.isNullOrBlank()) return@launch

            val urlJsonArray = JSONArray(urlJsonArrayStr)
            (0 until urlJsonArray.length()).forEach { i ->
                val obj = urlJsonArray.getJSONObject(i)
                urlList.add(
                    UrlData(
                        obj.optString(URL_SEARCH_HISTORY_URL_KEY),
                        obj.optInt(URL_SEARCH_HISTORY_COUNT_KEY),
                        obj.optLong(URL_SEARCH_HISTORY_TIME_KEY)
                    )
                )
            }

            urlList.sortWith(compareByDescending<UrlData> { it.count } //sort with priority
                .thenBy { it.time }
                .thenBy { it.url }
            )
        }
        return urlList
    }

    fun startWebBrowsing() {
        _actionEvent.value = Event("launch_url")
    }

    fun repeatNotifyWork() {
        notifyPeriodically(10_000L)
    }

    fun stopNotifyWork() {
        jobNotify?.cancel("notify work stopped")
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

    private fun notifyPeriodically(periodTime: Long = DEFAULT_PERIOD_TIME) {
        jobNotify = viewModelScope.launch {
            repeat(Int.MAX_VALUE) {
                Log.d("timer_web", "notifyPeriodically")
                delay(periodTime)
                _actionEvent.value = Event("notify_on_period")
            }
        }
    }

    // todo 신규 정의/호출해야 하는 함수 목록
    // timer 10초 갱신시마다(매초 보다는 효율적일듯) 현재 타이머의 값 저장하기
    // 앱을 구동시마다 로컬 저장소 값 기준으로 그래프 표현하기

    data class UrlData(
        var url: String,
        var count: Int,
        var time: Long
    )

    companion object {
        const val URL_SEARCH_HISTORY_PREF_KEY = "url_search_history"
        const val URL_SEARCH_HISTORY_URL_KEY = "url"
        const val URL_SEARCH_HISTORY_COUNT_KEY = "count"
        const val URL_SEARCH_HISTORY_TIME_KEY = "time"

        const val DEFAULT_PERIOD_TIME = 1000L * 60L * 10L // 10분
    }
}