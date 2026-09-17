package com.worklog.quickrecord.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.worklog.quickrecord.MainActivity
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.reminder.ReminderRules
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 桌面小组件：显示距离上次导出过了多久。
 *
 * 这一层是提醒机制的第二道。相比改应用图标，小组件能自由刷新、能带文字，
 * 也不会因为国产系统桌面不支持动态图标而丢图标。
 *
 * 版式参考笔记与待办类应用的小组件：一行小标题、一个放大的数字做主视觉、
 * 一行补充说明；超过阈值时数字与圆点一起变成橙色。
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
    val isStale = days != null && days >= ReminderRules.STALE_DAYS

    // 提醒色不跟随主题：需要在浅色和深色桌面上都能看清，
    // 所以取一个中间明度的橙，而不是主题里的强调色。
    val accent = ColorProvider(Color(0xFFD08A2A))
    val muted = GlanceTheme.colors.onSurfaceVariant
    val strong = GlanceTheme.colors.onSurface

    val openApp: Action = actionStartActivity(Intent(context, MainActivity::class.java))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(openApp),
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .background(accent)
                    .cornerRadius(4.dp),
            ) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "工作快录",
                style = TextStyle(color = muted, fontSize = 12.sp),
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        Row(verticalAlignment = Alignment.Vertical.Bottom) {
            Text(
                text = days?.toString() ?: "还没有",
                style = TextStyle(
                    color = if (isStale) accent else strong,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = if (days == null) "导出过" else "天未导出",
                style = TextStyle(color = muted, fontSize = 13.sp),
            )
        }

        Spacer(GlanceModifier.height(4.dp))

        Text(
            text = lastExport?.let { "上次导出 $it" } ?: "记一条，月底一键导出台账",
            style = TextStyle(color = muted, fontSize = 11.sp),
            maxLines = 1,
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
