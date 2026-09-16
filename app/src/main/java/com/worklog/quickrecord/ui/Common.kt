package com.worklog.quickrecord.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.worklog.quickrecord.data.PhotoStore
import com.worklog.quickrecord.util.BitmapDecoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 照片目录由界面容器提供，避免在每个界面里层层传参。 */
val LocalPhotoStore = staticCompositionLocalOf<PhotoStore> {
    error("PhotoStore 未注入")
}

/**
 * 显示一张本地照片。
 *
 * 解码放在 IO 线程并做采样，原图动辄几千像素，直接在组合阶段解码会卡住界面。
 */
@Composable
fun LocalPhoto(
    relativePath: String,
    modifier: Modifier = Modifier,
    maxSize: Int = 512,
) {
    val store = LocalPhotoStore.current
    var bitmap by remember(relativePath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(relativePath) {
        val decoded = withContext(Dispatchers.IO) {
            BitmapDecoding.decodeSampled(store.fileFor(relativePath), maxSize)
        }
        bitmap = decoded?.asImageBitmap()
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
