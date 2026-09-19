package com.worklog.quickrecord.reminder

import android.content.Context
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.domain.ExportAction
import com.worklog.quickrecord.widget.WidgetRefresh
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 记下「用户真的把报告导出出去了」。
 *
 * 提醒条、每月通知和桌面小组件都盯着这个日期，所以只在这里更新，
 * 而且只在用户真的保存或分享之后才调用：光是生成报告看预览不算导出。
 */
object ExportStamp {

    /**
     * @param action 用户是怎么把报告拿出去的，只看预览的 [ExportAction.Preview] 会被忽略
     */
    suspend fun mark(context: Context, action: ExportAction, today: LocalDate = LocalDate.now()) {
        if (!action.countsAsExport) return
        val appContext = context.applicationContext
        val preferences = Preferences(appContext)
        // 同一天里反复导出（比如分享时对方 App 读了好几次文件）只记一次，
        // 省掉重复写盘和重复刷新小组件。
        if (preferences.lastExportDate.first() == today) return
        preferences.setLastExportDate(today)
        WidgetRefresh.refresh(appContext)
    }
}
