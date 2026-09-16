package com.worklog.quickrecord.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.util.BitmapDecoding
import com.worklog.quickrecord.util.DisplayFormat
import com.worklog.quickrecord.util.ReportLayout
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * 把一段时间范围内的记录导出成 PDF 图文报告。
 *
 * 用系统自带的 PdfDocument 绘制，不引入第三方文档库；中文换行交给 StaticLayout，
 * 直接用 Canvas.drawText 排段落的话，长句子不会自动折行。
 * 照片以位图形式直接嵌进页面，报告不依赖原始图片文件，用户之后清理照片也不影响已导出的报告。
 */
object ReportExporter {

    // A4 在 72dpi 下是 595 × 842 点。
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    private const val COLUMN_GAP = ReportLayout.COLUMN_GAP
    private const val PHOTO_GAP = 8f
    private const val PLACE_LINE_HEIGHT = 16f
    private const val DESCRIPTION_GAP = 4f
    private const val PHOTOS_PER_ROW = 2

    private val titlePaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 17f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val metaPaint = TextPaint().apply {
        color = Color.rgb(110, 116, 113)
        textSize = 9f
        isAntiAlias = true
    }

    private val placePaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val bodyPaint = TextPaint().apply {
        color = Color.rgb(40, 48, 45)
        textSize = 9.5f
        isAntiAlias = true
    }

    private val rulePaint = Paint().apply {
        color = Color.rgb(226, 231, 229)
        strokeWidth = 0.8f
    }

    data class Result(
        val file: File,
        val pageCount: Int,
        val recordCount: Int,
        val photoCount: Int,
    )

    private class RecordLayouts(
        val description: StaticLayout,
        val photoRows: List<List<ReportLayout.Size>>,
    )

    /**
     * @param resolvePhoto 把数据库里的相对路径换算成真实文件
     */
    fun export(
        records: List<Record>,
        output: File,
        from: LocalDate,
        to: LocalDate,
        resolvePhoto: (String) -> File,
    ): Result {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var cursorY = drawHeader(canvas, from, to, records.size)
        var photoCount = 0

        records.forEachIndexed { index, record ->
            val layouts = buildLayouts(record)
            // 保持与 record.photos 一一对应，某张解码失败时不会让后面的图片错位。
            val photos: List<Bitmap?> = record.photos.map { photo ->
                BitmapDecoding.decodeSampled(resolvePhoto(photo.relativePath), 1200)
            }

            // 空间不够就翻页，一条记录不跨页截断。
            val needed = measureBlock(layouts)
            if (cursorY + needed > PAGE_HEIGHT - MARGIN && cursorY > MARGIN + 40f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(pageInfo(pageNumber))
                canvas = page.canvas
                cursorY = MARGIN
            }

            cursorY = drawRecord(canvas, record, layouts, photos, cursorY)
            photoCount += photos.count { it != null }
            photos.forEach { it?.recycle() }

            if (index != records.lastIndex) {
                canvas.drawLine(MARGIN, cursorY + 8f, PAGE_WIDTH - MARGIN, cursorY + 8f, rulePaint)
                cursorY += 20f
            }
        }

        document.finishPage(page)

        output.parentFile?.mkdirs()
        FileOutputStream(output).use { stream -> document.writeTo(stream) }
        document.close()

        return Result(output, pageNumber, records.size, photoCount)
    }

    private fun pageInfo(number: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), number).create()

    private fun drawHeader(canvas: Canvas, from: LocalDate, to: LocalDate, count: Int): Float {
        val title = "工作记录"
        canvas.drawText(title, (PAGE_WIDTH - titlePaint.measureText(title)) / 2f, MARGIN + 14f, titlePaint)

        val meta = "$from 至 $to · 共 $count 条"
        canvas.drawText(meta, (PAGE_WIDTH - metaPaint.measureText(meta)) / 2f, MARGIN + 32f, metaPaint)

        canvas.drawLine(MARGIN, MARGIN + 44f, PAGE_WIDTH - MARGIN, MARGIN + 44f, rulePaint)
        return MARGIN + 64f
    }

    private fun buildLayouts(record: Record): RecordLayouts {
        val description = StaticLayout.Builder
            .obtain(record.description, 0, record.description.length, bodyPaint, CONTENT_WIDTH.toInt())
            .setLineSpacing(0f, 1.35f)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

        val rows = record.photos.chunked(PHOTOS_PER_ROW).map { row ->
            row.map { photo -> photoSizeFor(photo, row.size) }
        }
        return RecordLayouts(description, rows)
    }

    private fun photoSizeFor(
        photo: com.worklog.quickrecord.domain.Photo,
        perRow: Int,
    ): ReportLayout.Size = ReportLayout.photoSize(
        contentWidth = CONTENT_WIDTH,
        perRow = perRow,
        photoWidth = photo.width,
        photoHeight = photo.height,
    )

    private fun measureBlock(layouts: RecordLayouts): Float {
        var height = PLACE_LINE_HEIGHT + DESCRIPTION_GAP + layouts.description.height.toFloat()
        if (layouts.photoRows.isNotEmpty()) {
            height += PHOTO_GAP
            layouts.photoRows.forEachIndexed { index, row ->
                height += row.maxOfOrNull { it.height } ?: 0f
                if (index != layouts.photoRows.lastIndex) height += PHOTO_GAP
            }
        }
        return height
    }

    private fun drawRecord(
        canvas: Canvas,
        record: Record,
        layouts: RecordLayouts,
        photos: List<Bitmap?>,
        startY: Float,
    ): Float {
        var y = startY

        canvas.drawText(record.place, MARGIN, y + 10f, placePaint)
        val time = DisplayFormat.full(record.occurredAt)
        canvas.drawText(time, PAGE_WIDTH - MARGIN - metaPaint.measureText(time), y + 10f, metaPaint)
        y += PLACE_LINE_HEIGHT + DESCRIPTION_GAP

        canvas.save()
        canvas.translate(MARGIN, y)
        layouts.description.draw(canvas)
        canvas.restore()
        y += layouts.description.height

        if (photos.isNotEmpty()) {
            y += PHOTO_GAP
            var bitmapIndex = 0
            layouts.photoRows.forEachIndexed { rowIndex, row ->
                val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() +
                    COLUMN_GAP * (row.size - 1).coerceAtLeast(0)
                var x = MARGIN + ReportLayout.rowStartX(CONTENT_WIDTH, rowWidth)
                var rowHeight = 0f
                row.forEach { size ->
                    val bitmap = photos.getOrNull(bitmapIndex)
                    if (bitmap != null) {
                        canvas.drawBitmap(
                            bitmap,
                            null,
                            RectF(x, y, x + size.width, y + size.height),
                            null,
                        )
                    }
                    bitmapIndex += 1
                    x += size.width + COLUMN_GAP
                    rowHeight = maxOf(rowHeight, size.height)
                }
                y += rowHeight
                if (rowIndex != layouts.photoRows.lastIndex) y += PHOTO_GAP
            }
        }
        return y
    }
}
