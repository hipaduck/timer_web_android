package com.hipaduck.timerweb.viewmodel

import android.text.format.DateUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    var timerSec = 0
    private val _timer = MutableLiveData<String>()
    val timer: MutableLiveData<String>
        get() = _timer

    init {
        viewModelScope.launch {
            while (timerSec >= 0) {
                _timer.value = DateUtils.formatElapsedTime((++timerSec).toLong())
//                Log.d("GAEGUL", "_timer.value: ${timer.value}")
                delay(1000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}