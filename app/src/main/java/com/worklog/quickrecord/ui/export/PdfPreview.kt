package com.worklog.quickrecord.ui.export

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worklog.quickrecord.R
import com.worklog.quickrecord.ui.theme.LocalAppColors
import com.worklog.quickrecord.util.ReportLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** 预览最多渲染这么多页，超出的部分不画图，只给一句提示。 */
private const val MAX_PREVIEW_PAGES = 20

private class Preview(val pages: List<ImageBitmap>, val totalPages: Int)

/**
 * 报告预览：把生成好的 PDF 逐页渲染成图片。
 *
 * 必须渲染全部页面：报告往往不止一页，只画第一页的话，
 * 后面的记录在预览里根本看不到，会让人误以为内容没更新。
 */
@Composable
fun PdfPreview(file: File, modifier: Modifier = Modifier) {
    // 用「路径 + 修改时间」做键：报告文件名是固定的，重复生成时路径不变，
    // 只按路径缓存会导致预览一直停在上一次生成的画面上。
    val cacheKey = file.path + ":" + file.lastModified()
    var preview by remember(cacheKey) { mutableStateOf(Preview(emptyList(), 0)) }

    LaunchedEffect(cacheKey) {
        preview = withContext(Dispatchers.IO) { renderPreview(file) }
    }

    val colors = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (preview.pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            preview.pages.forEach { page ->
                Image(
                    bitmap = page,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            // 页数太多时只画前面这些，明说一句，别让人以为后面的记录丢了。
            if (preview.totalPages > preview.pages.size) {
                Text(
                    text = stringResource(
                        R.string.export_preview_truncated,
                        preview.totalPages,
                        preview.pages.size,
                    ),
                    fontSize = 12.sp,
                    color = colors.sub,
                )
            }
        }
    }
}

private fun renderPreview(file: File): Preview = try {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        val renderer = PdfRenderer(descriptor)
        try {
            val total = renderer.pageCount
            val count = minOf(total, MAX_PREVIEW_PAGES)
            Preview(
                pages = (0 until count).mapNotNull { index -> renderPage(renderer, index, count) },
                totalPages = total,
            )
        } finally {
            renderer.close()
        }
    }
} catch (error: Exception) {
    Preview(emptyList(), 0)
}

/**
 * 渲染单独一页。某一页渲染失败只丢这一页，不影响其他页面显示，
 * 否则一整份报告的预览会集体变空白。
 */
private fun renderPage(renderer: PdfRenderer, index: Int, pageCount: Int): ImageBitmap? {
    val page = renderer.openPage(index)
    return try {
        val scale = ReportLayout.previewScale(pageCount, page.width, page.height)
        val width = maxOf(1, (page.width * scale).roundToInt())
        val height = maxOf(1, (page.height * scale).roundToInt())
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }.asImageBitmap()
    } catch (error: Exception) {
        null
    } finally {
        page.close()
    }
}
