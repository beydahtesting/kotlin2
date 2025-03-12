package com.example.myapplication.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun compressToJPEG(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    // Combines two Bitmaps vertically.
    fun combineBitmapsVertically(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
        val width = maxOf(bmp1.width, bmp2.width)
        val height = bmp1.height + bmp2.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bmp1, 0f, 0f, null)
        canvas.drawBitmap(bmp2, 0f, bmp1.height.toFloat(), null)
        return result
    }
}
