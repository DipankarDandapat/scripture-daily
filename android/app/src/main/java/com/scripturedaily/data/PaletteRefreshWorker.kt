package com.scripturedaily.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/** Legacy worker retained only to cancel jobs created by older app versions. */
class PaletteRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()

    companion object {
        private const val WORK_NAME = "scripture-daily-palette-boundary"
        private const val LEGACY_PERIODIC_WORK_NAME = "scripture-daily-palette-refresh"

        /** The independent palette scheduler is retired; VerseSyncWorker owns the single cadence. */
        fun schedule(context: Context) = cancel(context)

        fun cancel(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(WORK_NAME)
            manager.cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
        }
    }
}
