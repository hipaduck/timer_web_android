package com.hipaduck.timerweb.data

interface TimerWebRepository {
    suspend fun putValidDates(list: List<String>)

    suspend fun getValidDates(): List<String>

    suspend fun putValueFromDateKey(dateKey: String, sec: Int)

    suspend fun getValueFromDateKey(dateKey: String): Int
}