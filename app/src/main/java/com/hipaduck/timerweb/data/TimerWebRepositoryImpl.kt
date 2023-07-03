package com.hipaduck.timerweb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hipaduck.timerweb.SearchUrl
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerWebRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : TimerWebRepository {
    private val validDatePreferenceKey = stringPreferencesKey(VALID_DATE_PREF_KEY)

    // 유효한 날짜를 가지고 있는 valid_dates(시간역순정렬)
    // e.g. 20230613,20230612,20230610
    // 최대 7개를 가질 수 있으며 7개를 초과하는 경우 가장 오래된 날짜부터 삭제함
    override suspend fun putValidDates(list: List<String>) {
        if (list.size > 7) {
            list.sorted().take(list.size - 7).forEach {
                dataStore.edit { preferences ->
                    preferences.remove(longPreferencesKey(it))
                }
            }
        }
        val dataStr = list.map { it.isNotEmpty() }.sortedDescending().take(7).joinToString(",")
        dataStore.edit { preferences ->
            preferences[validDatePreferenceKey] = dataStr
        }
    }

    override suspend fun getValidDates(): List<String> {
        val storedDataStr = dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }.map { pref ->
            pref[validDatePreferenceKey] ?: ""
        }.first()
        return storedDataStr.split(",")
    }

    // 각 날짜를 키로하여 숫자로 머무른 초 기록(기록시 초 단위로 올림 처리함, 1.3초일 경우 2초로 기록)
    // e.g. 키는 20230610, 값은 1211, 키는 20230612, 값은 223
    // 값은 매번 기록하지 않고, 메모리로 가지고 있다가, 해당 앱을 나가는 시점이나 변화하는 시점에 기록
    override suspend fun putValueFromDateKey(dateKey: String, sec: Long) {
        val currentValue = dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }.map { pref ->
            pref[longPreferencesKey(dateKey)] ?: 0
        }.first()
        val targetValue = currentValue + sec
        dataStore.edit { preferences ->
            preferences[longPreferencesKey(dateKey)] = targetValue
        }
    }

    override suspend fun getValueFromDateKey(dateKey: String): Long =
        dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }.map { pref ->
            pref[longPreferencesKey(dateKey)] ?: 0
        }.first()

    override suspend fun putSearchUrl(url: String) {
        val urlJsonArrayStr =
            dataStore.data.map {
                it[stringPreferencesKey(
                    URL_HISTORY_PREF_KEY
                )]
            }.first()

        var urlJsonArray = JSONArray()
        var jsonObject = JSONObject()
            .put(URL_HISTORY_COUNT_KEY, 0)
            .put(URL_HISTORY_TIME_KEY, System.currentTimeMillis())
            .put(URL_HISTORY_URL_KEY, url)
        var foundIndex: Int? = null
        if (!urlJsonArrayStr.isNullOrEmpty()) {
            urlJsonArray = JSONArray(urlJsonArrayStr)
            (0 until urlJsonArray.length()).forEach { i ->
                val obj = urlJsonArray.optJSONObject(i)
                if (obj.optString(URL_HISTORY_URL_KEY).equals(url)) {
                    jsonObject = JSONObject()
                        .put(
                            URL_HISTORY_COUNT_KEY,
                            obj.optInt(URL_HISTORY_COUNT_KEY) + 1
                        )
                        .put(URL_HISTORY_TIME_KEY, System.currentTimeMillis())
                        .put(URL_HISTORY_URL_KEY, url)
                    foundIndex = i
                }
            }
        }
        foundIndex?.let {
            urlJsonArray.remove(it)
        }
        urlJsonArray.put(jsonObject)
        dataStore.edit {
            it[stringPreferencesKey(
                URL_HISTORY_PREF_KEY
            )] = urlJsonArray.toString()
        }
    }

    override suspend fun getSearchUrl(): List<SearchUrl> {
        val urlList = mutableListOf<SearchUrl>()
        val urlJsonArrayStr =
            dataStore.data.map {
                it[stringPreferencesKey(
                    URL_HISTORY_PREF_KEY
                )]
            }.first()

        if (urlJsonArrayStr.isNullOrBlank()) return urlList

        val urlJsonArray = JSONArray(urlJsonArrayStr)
        (0 until urlJsonArray.length()).forEach { i ->
            val obj = urlJsonArray.getJSONObject(i)
            urlList.add(
                SearchUrl(
                    obj.optString(URL_HISTORY_URL_KEY),
                    obj.optInt(URL_HISTORY_COUNT_KEY),
                    obj.optLong(URL_HISTORY_TIME_KEY)
                )
            )
        }

        urlList.sortWith(compareByDescending<SearchUrl> { it.count } //sort with priority: count > time > url
            .thenBy { it.time }
            .thenBy { it.url }
        )

        return urlList
    }

    companion object {
        const val VALID_DATE_PREF_KEY = "valid_dates"
        const val URL_HISTORY_PREF_KEY = "url_search_history"
        const val URL_HISTORY_URL_KEY = "url"
        const val URL_HISTORY_COUNT_KEY = "count"
        const val URL_HISTORY_TIME_KEY = "time"
    }
}