package com.worklog.quickrecord.domain

/**
 * 用户对报告做过的动作。
 *
 * 抽成枚举是为了把「什么才算真的导出过」写在一个地方：提醒条、每月通知和
 * 桌面小组件都盯着「上次导出时间」，如果只是生成报告预览一下就把它刷新，
 * 用户明明没往外导出、提醒却消失了，提醒就形同虚设。
 */
enum class ExportAction {
    /** 生成报告，只是为了在 App 里看预览。 */
    Preview,

    /** 把报告保存成文件。 */
    SaveFile,

    /** 把报告分享给别人。 */
    Share,
    ;

    /** 真的把报告拿出去过，才算一次导出。 */
    val countsAsExport: Boolean get() = this != Preview
}
