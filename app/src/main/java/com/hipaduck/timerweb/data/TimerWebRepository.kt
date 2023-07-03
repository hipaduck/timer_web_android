package com.hipaduck.timerweb.data

import com.hipaduck.timerweb.model.SearchUrl

interface TimerWebRepository {
    suspend fun putValidDates(list: List<String>)

    suspend fun getValidDates(): List<String>

    suspend fun accumulateValueFromDateKey(dateKey: String, sec: Long)

    suspend fun getValueFromDateKey(dateKey: String): Long

    suspend fun putSearchUrl(url: String)

    suspend fun getSearchUrl(): List<SearchUrl>
}