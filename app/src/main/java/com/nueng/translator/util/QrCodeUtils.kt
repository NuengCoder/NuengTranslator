package com.nueng.translator.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.scale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.nueng.translator.R
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QrCodeUtils {

    /**
     * Generates a QR code bitmap for the given content.
     * Overlays the app launcher icon in the center (30% of QR size).
     */
    fun generateQrWithIcon(context: Context, content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
        )

        val writer  = QRCodeWriter()
        val matrix  = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp     = createBitmap(sizePx, sizePx)

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        // Overlay app icon in center
        return try {
            val iconRaw = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            val iconSize = (sizePx * 0.22f).toInt()
            val iconScaled = iconRaw.scale(iconSize, iconSize, filter = true)

            val result = bmp.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)

            // White rounded background behind icon
            val padding = (iconSize * 0.15f)
            val left   = (sizePx - iconSize) / 2f - padding
            val top    = (sizePx - iconSize) / 2f - padding
            val right  = left + iconSize + padding * 2
            val bottom = top  + iconSize + padding * 2
            val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(RectF(left, top, right, bottom), padding, padding, paint)

            // Draw icon
            val iconLeft = (sizePx - iconSize) / 2f
            val iconTop  = (sizePx - iconSize) / 2f
            canvas.drawBitmap(iconScaled, iconLeft, iconTop, null)
            iconScaled.recycle()

            result
        } catch (_: Exception) {
            bmp // fallback: QR without icon
        }
    }
}
