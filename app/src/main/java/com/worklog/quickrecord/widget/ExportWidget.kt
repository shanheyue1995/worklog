package com.worklog.quickrecord.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.worklog.quickrecord.MainActivity
import com.worklog.quickrecord.data.Preferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 桌面小组件：显示距离上次导出过了多久。
 *
 * 这一层是提醒机制的第二道。相比改应用图标，小组件能自由刷新、能带文字，
 * 也不会因为国产系统桌面不支持动态图标而丢图标。
 */
class ExportWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lastExport = Preferences(context).lastExportDate.first()
        provideContent {
            GlanceTheme {
                WidgetContent(lastExport, context)
            }
        }
    }
}

@Composable
private fun WidgetContent(lastExport: LocalDate?, context: Context) {
    val days = lastExport?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }
    val openApp: Action = actionStartActivity(Intent(context, MainActivity::class.java))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(14.dp)
            .clickable(openApp),
    ) {
        Text(
            text = "工作快录",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
            ),
        )
        Text(
            text = if (days == null) "还没导出过" else "$days 天未导出",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = if (lastExport == null) "记录几条之后导出一份试试" else "上次导出：$lastExport",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
            ),
        )
    }
}

class ExportWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExportWidget()
}

/** 导出完成或数据变化后刷新所有小组件实例。 */
object WidgetRefresh {
    suspend fun refresh(context: Context) {
        ExportWidget().updateAll(context)
    }
}
