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

class StudentImageViewModel : ViewModel() {
    var studentBitmap = mutableStateOf<Bitmap?>(null) // ✅ Use MutableState
        private set

    var isProcessing = mutableStateOf(false)
        private set

    // ✅ **Load cached student image only if it's not manually cleared**
    fun loadCachedImage(context: Context) {
        if (studentBitmap.value == null) { // ✅ Prevents loading cache if manually cleared
            viewModelScope.launch(Dispatchers.IO) {
                val cachedImage = ImageCache.getStudentImage(context)
                withContext(Dispatchers.Main) {
                    studentBitmap.value = cachedImage
                }
            }
        }
    }

    // ✅ **Process student image from gallery**
    fun processStudentImage(context: Context, selectedBitmap: Bitmap) {
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
                    studentBitmap.value = processedBitmap
                    ImageCache.setStudentImage(context, processedBitmap)
                    isProcessing.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    studentBitmap.value = originalBitmap // ✅ Restore original image if processing fails
                    ImageCache.setStudentImage(context, originalBitmap) // ✅ Save original image in cache
                    isProcessing.value = false
                }
            }
        }
    }


    // ✅ **Rotate Student Image**
    fun rotateImage(context: Context) {
        studentBitmap.value?.let { bmp ->
            val rotatedBitmap = ImageTransforms.rotateImage(bmp, 90.0)
            studentBitmap.value = rotatedBitmap
            ImageCache.setStudentImage(context, rotatedBitmap)
        }
    }

    // ✅ **Flip Student Image**
    fun flipImage(context: Context) {
        studentBitmap.value?.let { bmp ->
            val flippedBitmap = ImageTransforms.flipImage(bmp)
            studentBitmap.value = flippedBitmap
            ImageCache.setStudentImage(context, flippedBitmap)
        }
    }

    // ✅ **Clear only student image (Fixes unwanted cache loading)**
    fun clearStudentImage() {
        studentBitmap.value = null // ✅ Prevents cache from loading when opening screen
    }
}
