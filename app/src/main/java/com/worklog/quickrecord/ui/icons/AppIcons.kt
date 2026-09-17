package com.worklog.quickrecord.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 设计稿里指定的两个图标，用矢量路径精确还原。
 * 颜色留成黑色，使用处通过 tint 覆盖。
 */

/** 导出：开口方框 + 斜穿而出的箭头 */
val ExportIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Export",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M11.4 7.2H4.7a2.1 2.1 0 0 0-2.1 2.1v9.4a2.1 2.1 0 0 0 2.1 2.1h14.6a2.1 2.1 0 0 0 2.1-2.1v-6.6" +
                    "M9.8 11 20.2 3.7" +
                    "M14 3.7h6.2V10",
            ).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}

/** 修改时间：开口方框 + 斜放的橡皮 */
val EraserIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Eraser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M13.4 4.6H6.6A2.2 2.2 0 0 0 4.4 6.8v10.8a2.2 2.2 0 0 0 2.2 2.2h10.8a2.2 2.2 0 0 0 2.2-2.2v-5.4",
            ).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        addGroup(name = "eraser", rotate = -38f, pivotX = 15.15f, pivotY = 8.15f)
        addPath(
            pathData = PathParser().parsePathString(roundRect(8.8f, 5.5f, 4.2f, 5.3f, 1.2f)).toNodes(),
            fill = SolidColor(Color.Black),
        )
        addPath(
            pathData = PathParser().parsePathString(roundRect(13.9f, 5.5f, 7.6f, 5.3f, 1.2f)).toNodes(),
            fill = SolidColor(Color.Black),
        )
        clearGroup()
    }.build()
}

/** 返回：掉头箭头 */
val BackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Back",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M3.6 7.6h10.6a5.4 5.4 0 0 1 0 10.8H3.6" +
                    "M7.6 3.6 3.6 7.6l4 4",
            ).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}

/** 相机：机身 + 顶部取景凸起 + 镜头 */
val CameraIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M4.7 8.6h14.6a2.2 2.2 0 0 1 2.2 2.2v7.4a2.2 2.2 0 0 1-2.2 2.2H4.7a2.2 2.2 0 0 1-2.2-2.2v-7.4a2.2 2.2 0 0 1 2.2-2.2Z" +
                    "M7.9 8.6 9.3 5.4h5.4l1.4 3.2" +
                    "M12 10.4a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 1 0 0-7.2Z",
            ).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}

private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    fun n(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()

    return "M${n(x + r)} ${n(y)}H${n(x + w - r)}A${n(r)} ${n(r)} 0 0 1 ${n(x + w)} ${n(y + r)}" +
        "V${n(y + h - r)}A${n(r)} ${n(r)} 0 0 1 ${n(x + w - r)} ${n(y + h)}" +
        "H${n(x + r)}A${n(r)} ${n(r)} 0 0 1 ${n(x)} ${n(y + h - r)}" +
        "V${n(y + r)}A${n(r)} ${n(r)} 0 0 1 ${n(x + r)} ${n(y)}Z"
}
