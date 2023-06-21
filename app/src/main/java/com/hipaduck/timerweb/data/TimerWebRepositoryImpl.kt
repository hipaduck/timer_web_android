package com.hipaduck.timerweb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerWebRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : TimerWebRepository {
    private val validDatePreferenceKey = stringPreferencesKey("valid_dates")

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
}