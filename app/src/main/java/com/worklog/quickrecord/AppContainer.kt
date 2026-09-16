package com.worklog.quickrecord

import android.content.Context
import androidx.room.Room
import com.worklog.quickrecord.data.AppDatabase
import com.worklog.quickrecord.data.PhotoStore
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.data.RecordRepository
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
}
