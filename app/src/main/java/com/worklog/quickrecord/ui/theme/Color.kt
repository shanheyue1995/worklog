package com.worklog.quickrecord.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 界面配色。
 *
 * 取值来自《产品设计文档》第 5 节，与定稿的高保真原型一一对应：
 * 浅灰背景 + 纯白卡片 + 用细线而不是投影分层，主色仍是品牌深绿。
 */
data class AppColors(
    /** 页面底色 */
    val page: Color,
    /** 卡片表面 */
    val card: Color,
    /** 分隔细线 */
    val divider: Color,
    /** 主文字 */
    val ink: Color,
    /** 次要文字 */
    val sub: Color,
    /** 三级文字（摘要、提示） */
    val subSoft: Color,
    /** 列表左侧图标块底色 */
    val tile: Color,
    /** 图标块前景 */
    val tileForeground: Color,
    /** 品牌主色 */
    val brand: Color,
    /** 品牌浅底（用于次级按钮） */
    val brandSoft: Color,
    /** 提醒条底色 / 描边 / 标题 / 说明 */
    val warnContainer: Color,
    val warnOutline: Color,
    val warnContent: Color,
    val warnSupport: Color,
    /** 超期强调色（小组件圆点） */
    val accent: Color,
    /** 破坏性操作 */
    val danger: Color,
)

private val LightColors = AppColors(
    page = Color(0xFFF1F3F2),
    card = Color(0xFFFFFFFF),
    divider = Color(0xFFF0F1F0),
    ink = Color(0xFF191C1B),
    sub = Color(0xFF9AA0A6),
    subSoft = Color(0xFFB9BFC2),
    tile = Color(0xFFE9F1FD),
    tileForeground = Color(0xFF4A7FD4),
    brand = Color(0xFF136A52),
    brandSoft = Color(0xFFE3EFE9),
    warnContainer = Color(0xFFFDF4E3),
    warnOutline = Color(0xFFF2E3C4),
    warnContent = Color(0xFF8A5A12),
    warnSupport = Color(0xFFA08A63),
    accent = Color(0xFFD08A2A),
    danger = Color(0xFFB3261E),
)

private val DarkColors = AppColors(
    page = Color(0xFF0F1211),
    card = Color(0xFF1A1E1D),
    divider = Color(0xFF2A302E),
    ink = Color(0xFFE9EEEC),
    sub = Color(0xFF9AA5A0),
    subSoft = Color(0xFF6E7A75),
    tile = Color(0xFF1D2A3A),
    tileForeground = Color(0xFF8FB6F0),
    brand = Color(0xFF5FD3AA),
    brandSoft = Color(0xFF1C332B),
    warnContainer = Color(0xFF33280F),
    warnOutline = Color(0xFF4A3A18),
    warnContent = Color(0xFFE8B169),
    warnSupport = Color(0xFFB79A6A),
    accent = Color(0xFFE0A24A),
    danger = Color(0xFFF09A94),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

fun appColors(darkTheme: Boolean): AppColors = if (darkTheme) DarkColors else LightColors

// Material 组件（按钮、输入框、对话框）仍然需要 colorScheme，这里给出对应的主色
val MaterialPrimaryLight = Color(0xFF136A52)
val MaterialPrimaryDark = Color(0xFF5FD3AA)
