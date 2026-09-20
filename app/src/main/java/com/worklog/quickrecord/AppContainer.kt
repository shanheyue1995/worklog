package com.worklog.quickrecord

import android.content.Context
import androidx.room.Room
import com.worklog.quickrecord.data.AppDatabase
import com.worklog.quickrecord.data.BackupManager
import com.worklog.quickrecord.data.PhotoStore
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.data.RecordRepository
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.widget.WidgetRefresh
import com.worklog.quickrecord.widget.WidgetRefreshRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * 手写的应用级依赖容器。
 *
 * 第一版模块很少，引入依赖注入框架带来的构建复杂度大于收益，等模块数量明显
 * 增长再替换。所有依赖在这里集中构造，界面层只通过它拿仓库。
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database: AppDatabase = Room
        .databaseBuilder(appContext, AppDatabase::class.java, "quickrecord.db")
        .build()

    val photoStore: PhotoStore = PhotoStore(File(appContext.filesDir, "photos"))

    val preferences: Preferences = Preferences(appContext)

    /** 导出的报告放在缓存目录，系统清理时不影响记录本身。 */
    val exportDir: File = File(appContext.cacheDir, "exports").apply { mkdirs() }

    val recordRepository: RecordRepository = RecordRepository(database, photoStore)

    val backupManager: BackupManager = BackupManager(recordRepository, photoStore)

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // 记录一有变化就通知桌面小组件重画。
        //
        // 小组件是自己去数据库取数的，数据变了不会自动更新；以前只有导出时才刷新，
        // 所以新记一条之后，小组件上的「最近记录」会一直停在旧的那条上。
        // 挂在这里而不是各个保存按钮上，是因为恢复备份、删除记录也都会经过这个数据源。
        applicationScope.launch {
            var previous: List<Record>? = null
            recordRepository.observeRecords().collect { records ->
                val before = previous
                previous = records
                if (WidgetRefreshRules.shouldRefresh(before, records)) {
                    WidgetRefresh.refresh(appContext)
                }
            }
        }
    }
}
