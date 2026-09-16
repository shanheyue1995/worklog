package com.worklog.quickrecord.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.worklog.quickrecord.QuickRecordApp
import com.worklog.quickrecord.data.Preferences
import kotlinx.coroutines.flow.first

/**
 * 每月 1 号的导出提醒。
 *
 * 任务执行时会顺手把下一次排进去，所以只需要在应用启动时排一次。
 * 国产系统对后台任务限制较严，可能延迟甚至不执行，
 * 因此打开应用时的提示条才是可靠的那一道，这里是补充。
 */
class ExportReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val preferences = Preferences(context)

        if (preferences.reminderEnabled.first()) {
            val recordCount = (context as? QuickRecordApp)
                ?.container
                ?.recordRepository
                ?.countRecords()
                ?: 0
            if (recordCount > 0) {
                ReminderScheduler.notifyNow(context)
            }
        }

        ReminderScheduler.reschedule(context)
        return Result.success()
    }
}
