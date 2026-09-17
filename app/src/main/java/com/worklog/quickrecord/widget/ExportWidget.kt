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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.worklog.quickrecord.MainActivity
import com.worklog.quickrecord.QuickRecordApp
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.reminder.ReminderRules
import com.worklog.quickrecord.util.DisplayFormat
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 桌面小组件：上半是导出状态，下半是最近一条记录。
 *
 * 版式与 APP 内保持一致：白色圆角卡片、细线分隔、圆角徽标；
 * 超过阈值时圆点与徽标一起变成橙色。
 */
class ExportWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lastExport = Preferences(context).lastExportDate.first()
        val repository = (context.applicationContext as? QuickRecordApp)
            ?.container
            ?.recordRepository
        val latest = repository?.latest()
        provideContent {
            GlanceTheme {
                WidgetContent(lastExport, latest, context)
            }
        }
    }
}

@Composable
private fun WidgetContent(lastExport: LocalDate?, latest: Record?, context: Context) {
    val days = lastExport?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }
    val isStale = days == null || days >= ReminderRules.STALE_DAYS

    val accent = ColorProvider(if (isStale) Color(0xFFD08A2A) else Color(0xFF2A9C7B))
    val accentSoft = ColorProvider(if (isStale) Color(0x1FD08A2A) else Color(0x1F2A9C7B))
    val muted = GlanceTheme.colors.onSurfaceVariant
    val strong = GlanceTheme.colors.onSurface
    val hairline = ColorProvider(Color(0x1F888888))

    val openApp: Action = actionStartActivity(Intent(context, MainActivity::class.java))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(openApp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(7.dp)
                    .background(accent)
                    .cornerRadius(4.dp),
            ) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "工作快录",
                style = TextStyle(color = muted, fontSize = 12.sp),
            )
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(accentSoft)
                    .cornerRadius(999.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (days == null) "还没导出" else "$days 天未导出",
                    style = TextStyle(
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(hairline),
        ) {}

        Spacer(GlanceModifier.height(9.dp))
        Text(
            text = "最近记录",
            style = TextStyle(color = muted, fontSize = 10.5.sp),
        )
        Spacer(GlanceModifier.height(3.dp))

        if (latest == null) {
            Text(
                text = "记一条，月底一键导出台账",
                style = TextStyle(color = muted, fontSize = 12.5.sp),
            )
        } else {
            Text(
                text = "${latest.place} · ${DisplayFormat.short(latest.occurredAt)}",
                style = TextStyle(
                    color = strong,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = latest.description,
                style = TextStyle(color = muted, fontSize = 12.sp),
                maxLines = 2,
            )
        }
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
