package com.wuxianggujun.tinaide.core.apkbuilder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * 把用户挑选的图标文件解码、缩放到启动器图标基准尺寸，再编码为 PNG 字节。
 *
 * 当前策略：统一输出 [TARGET_PX] × [TARGET_PX] 的 PNG 覆盖模板中的所有密度桶。
 * 这虽然不是每个密度的最佳分辨率，但图标通常只有一个密度（template 仅 xxhdpi），
 * 出包后在各种设备上都能正确显示。
 */
internal object IconRasterizer {

    private const val TAG = "IconRasterizer"
    private const val TARGET_PX = 192

    fun rasterize(iconFile: File): ByteArray {
        if (!iconFile.isFile) {
            throw IOException("Icon file does not exist: ${iconFile.absolutePath}")
        }
        val bitmap = decodeFittedBitmap(iconFile)
            ?: throw IOException("Unable to decode icon file: ${iconFile.absolutePath}")
        try {
            val canvasBitmap = renderToTargetBitmap(bitmap)
            try {
                val buffer = ByteArrayOutputStream(canvasBitmap.byteCount.coerceAtLeast(16 * 1024))
                val compressed = canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, buffer)
                if (!compressed) {
                    throw IOException("Bitmap.compress returned false for icon: ${iconFile.name}")
                }
                return buffer.toByteArray()
            } finally {
                if (canvasBitmap !== bitmap) {
                    canvasBitmap.recycle()
                }
            }
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun decodeFittedBitmap(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Timber.tag(TAG).w("Cannot read bitmap bounds for %s", file.absolutePath)
            return null
        }
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_PX)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, loadOptions)
    }

    private fun computeInSampleSize(sourceWidth: Int, sourceHeight: Int, targetPx: Int): Int {
        var sample = 1
        var w = sourceWidth
        var h = sourceHeight
        while (w / 2 >= targetPx && h / 2 >= targetPx) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }

    private fun renderToTargetBitmap(source: Bitmap): Bitmap {
        if (source.width == TARGET_PX && source.height == TARGET_PX) {
            return source
        }
        val target = Bitmap.createBitmap(TARGET_PX, TARGET_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val scale = minOf(
            TARGET_PX.toFloat() / source.width,
            TARGET_PX.toFloat() / source.height
        )
        val drawWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val drawHeight = (source.height * scale).toInt().coerceAtLeast(1)
        val left = (TARGET_PX - drawWidth) / 2
        val top = (TARGET_PX - drawHeight) / 2
        val destRect = Rect(left, top, left + drawWidth, top + drawHeight)
        canvas.drawBitmap(source, null, destRect, paint)
        return target
    }
}
