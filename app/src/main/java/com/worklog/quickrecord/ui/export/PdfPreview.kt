package com.worklog.quickrecord.ui.export

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 报告预览：把生成好的 PDF 第一页直接渲染成图片。
 *
 * 这样预览看到的就是真实产物，而不是另画一份近似的界面稿。
 */
@Composable
fun PdfFirstPagePreview(file: File, modifier: Modifier = Modifier) {
    var image by remember(file.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file.path) {
        image = withContext(Dispatchers.IO) { renderFirstPage(file) }?.asImageBitmap()
    }

    val bitmap = image
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

private fun renderFirstPage(file: File): Bitmap? = try {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        val renderer = PdfRenderer(descriptor)
        try {
            val renderPage = renderer.openPage(0)
            try {
                // 按两倍尺寸渲染，缩略图在手机上才够清晰。
                Bitmap.createBitmap(
                    renderPage.width * 2,
                    renderPage.height * 2,
                    Bitmap.Config.ARGB_8888,
                ).also { bitmap ->
                    bitmap.eraseColor(Color.WHITE)
                    renderPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            } finally {
                renderPage.close()
            }
        } finally {
            renderer.close()
        }
    }
} catch (error: Exception) {
    null
}
