package com.worklog.quickrecord.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.worklog.quickrecord.util.ImageSizing
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

/**
 * 把一张原始照片处理成 App 内的正式照片。
 *
 * 三件事必须做对：按需采样避免内存爆掉、按方向标记转正、重新编码压缩体积。
 * 方向这一条最容易被忽略，不处理的话照片在列表、详情和导出的 PDF 里都会躺着。
 */
object PhotoImport {

    /** 长边上限，配合 JPEG 质量 85，单张约 200–400KB。 */
    const val MAX_LONG_SIDE = 1600
    const val JPEG_QUALITY = 85

    data class Imported(val relativePath: String, val width: Int, val height: Int)

    /**
     * @param source 原始照片，可能是系统相机的产物，也可能是从相册复制出来的临时文件
     */
    fun import(store: PhotoStore, source: File, occurredAt: LocalDateTime): Imported? {
        if (!source.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = ImageSizing.sampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    MAX_LONG_SIDE,
                )
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null

        val oriented = applyExifRotation(source, decoded)
        val (targetWidth, targetHeight) = ImageSizing.scaledSize(
            oriented.width,
            oriented.height,
            MAX_LONG_SIDE,
        )
        val scaled = if (targetWidth != oriented.width || targetHeight != oriented.height) {
            Bitmap.createScaledBitmap(oriented, targetWidth, targetHeight, true).also {
                if (it != oriented) oriented.recycle()
            }
        } else {
            oriented
        }

        val relativePath = store.newRelativePath(occurredAt)
        val target = store.fileFor(relativePath)
        target.parentFile?.mkdirs()

        val written = try {
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        } catch (error: Exception) {
            false
        }

        val width = scaled.width
        val height = scaled.height
        scaled.recycle()
        store.deleteFile(source)

        return if (written) Imported(relativePath, width, height) else null
    }

    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
        val degrees = try {
            when (ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (error: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
}
