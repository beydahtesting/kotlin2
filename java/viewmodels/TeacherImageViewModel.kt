package com.example.myapplication.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.processing.ImageProcessor
import com.example.myapplication.processing.ImageTransforms
import com.example.myapplication.storage.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat

class TeacherImageViewModel : ViewModel() {
    var teacherBitmap = mutableStateOf<Bitmap?>(null) // ✅ Make it MutableState
        private set

    var isProcessing = mutableStateOf(false)
        private set

    // ✅ **Load cached teacher image when the screen opens**
    fun loadCachedImage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedImage = ImageCache.getTeacherImage(context)
            withContext(Dispatchers.Main) {
                teacherBitmap.value = cachedImage
            }
        }
    }

    fun processTeacherImage(context: Context, selectedBitmap: Bitmap) {
        isProcessing.value = true
        val originalBitmap = selectedBitmap // ✅ Store original image before processing

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mat = Mat().also { Utils.bitmapToMat(selectedBitmap, it) }
                val processedMat = ImageProcessor.processImage(mat)
                val finalMat = ImageProcessor.drawDetectedCircles(processedMat)
                val processedBitmap = Bitmap.createBitmap(finalMat.cols(), finalMat.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(finalMat, processedBitmap)

                withContext(Dispatchers.Main) {
                    teacherBitmap.value = processedBitmap
                    ImageCache.setTeacherImage(context, processedBitmap)
                    isProcessing.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    teacherBitmap.value = originalBitmap // ✅ Restore original image if processing fails
                    ImageCache.setTeacherImage(context, originalBitmap) // ✅ Save original image in cache
                    isProcessing.value = false
                }
            }


        }
    }

    // ✅ Rotate Image
    fun rotateImage(context: Context) {
        teacherBitmap.value?.let { bmp ->
            val rotatedBitmap = ImageTransforms.rotateImage(bmp, 90.0)
            teacherBitmap.value = rotatedBitmap
            ImageCache.setTeacherImage(context, rotatedBitmap)
        }
    }

    // ✅ Flip Image
    fun flipImage(context: Context) {
        teacherBitmap.value?.let { bmp ->
            val flippedBitmap = ImageTransforms.flipImage(bmp)
            teacherBitmap.value = flippedBitmap
            ImageCache.setTeacherImage(context, flippedBitmap)
        }
    }
}
