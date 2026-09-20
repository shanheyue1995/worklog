package com.worklog.quickrecord.widget

import com.worklog.quickrecord.domain.Record

/**
 * 什么时候需要重画桌面小组件。
 *
 * 小组件上只有两样东西：「X 天未导出」（跟着导出时间走，导出时已经刷新）
 * 和「最近一条记录」。记录列表是按时间倒序的，所以只有当**第一条**变了才
 * 需要重画：改一条旧记录、或者补录一条以前的记录，小组件显示的内容其实没变。
 *
 * 抽成纯函数是为了能在电脑上直接验，不用连真机看小组件。
 */
object WidgetRefreshRules {

    /**
     * @param previous 上一次看到的记录列表；null 表示应用刚启动，还没有基准
     */
    fun shouldRefresh(previous: List<Record>?, current: List<Record>): Boolean {
        // 启动后的第一次读取也刷一次，免得小组件停在上次留下的旧画面上
        if (previous == null) return true
        return previous.firstOrNull() != current.firstOrNull()
    }
}
