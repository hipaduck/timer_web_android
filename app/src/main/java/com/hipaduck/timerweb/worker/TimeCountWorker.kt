package com.hipaduck.timerweb.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TimeCountWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result =
        try {
            counter += 10
            Log.d("TimeCountWorker", "doWork: $counter")

            // counter를 preference에 기록

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }

    companion object {
        var counter = 0
    }
}