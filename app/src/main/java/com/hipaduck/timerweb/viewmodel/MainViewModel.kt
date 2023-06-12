package com.hipaduck.timerweb.viewmodel

import android.text.format.DateUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hipaduck.timerweb.data.TimerWebRepository
import com.hipaduck.timerweb.worker.TimeCountWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val timerWebRepository: TimerWebRepository,
    private val workManager: WorkManager
) : ViewModel() {
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

    fun startWork() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .build()

        // todo PeriodicWork를 사용하기 위해서는 최소시간이 15분이다... 이걸 어찌 활용해야하나..
        val workRequest = PeriodicWorkRequestBuilder<TimeCountWorker>(10, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORKER_KEY, ExistingPeriodicWorkPolicy.UPDATE, workRequest
        )
    }

    fun cancelWork() = workManager.cancelUniqueWork(WORKER_KEY)


    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private const val WORKER_KEY = "time_count_worker"
    }
}