package com.example.myapplication.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.AppMode
import com.example.myapplication.models.StudentRecord
import com.example.myapplication.processing.ImageProcessor
import com.example.myapplication.storage.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot

class ResultViewModel : ViewModel() {

    private val _gradedBitmap = MutableStateFlow<Bitmap?>(null)
    val gradedBitmap: StateFlow<Bitmap?> = _gradedBitmap

    private val _teacherBitmap = MutableStateFlow<Bitmap?>(null)
    val teacherBitmap: StateFlow<Bitmap?> = _teacherBitmap

    private val _resultText = MutableStateFlow("")
    val resultText: StateFlow<String> = _resultText

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    // Zoom & Move state for Teacher Image
    private val _scaleTeacher = MutableStateFlow(1f)
    val scaleTeacher: StateFlow<Float> = _scaleTeacher

    private val _offsetXTeacher = MutableStateFlow(0f)
    val offsetXTeacher: StateFlow<Float> = _offsetXTeacher

    private val _offsetYTeacher = MutableStateFlow(0f)
    val offsetYTeacher: StateFlow<Float> = _offsetYTeacher

    // Zoom & Move state for Graded Image
    private val _scaleGraded = MutableStateFlow(1f)
    val scaleGraded: StateFlow<Float> = _scaleGraded

    private val _offsetXGraded = MutableStateFlow(0f)
    val offsetXGraded: StateFlow<Float> = _offsetXGraded

    private val _offsetYGraded = MutableStateFlow(0f)
    val offsetYGraded: StateFlow<Float> = _offsetYGraded

    private var recordAdded = false // Prevent duplicate record insertion

    // Load cached teacher image (assumed to have been saved by the teacher screen)
    fun loadCachedImages(context: Context) {
        _teacherBitmap.value = ImageCache.getTeacherImage(context)
    }

    // Process images if not already processed
    fun processImagesIfNeeded(context: Context, onProcessingComplete: () -> Unit) {
        // Prevent reprocessing if we already have a result.
        if (_gradedBitmap.value != null && _resultText.value.isNotEmpty()) return

        val studentBitmap = ImageCache.getStudentImage(context)
        val teacherBitmap = _teacherBitmap.value

        if (teacherBitmap == null || studentBitmap == null) {
            Log.e("ResultViewModel", "Missing images - cannot process")
            return
        }

        _isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert Bitmaps to Mats
                val teacherMat = Mat().also { Utils.bitmapToMat(teacherBitmap, it) }
                val studentMat = Mat().also { Utils.bitmapToMat(studentBitmap, it) }

                // Detect circles
                val teacherCircles = ImageProcessor.detectFilledCircles(teacherMat)
                val studentCircles = ImageProcessor.detectFilledCircles(studentMat)

                // Calculate correct answers
                val correctCount = studentCircles.count { studentAnswer ->
                    teacherCircles.any { teacherAnswer ->
                        hypot(teacherAnswer.x - studentAnswer.x, teacherAnswer.y - studentAnswer.y) < 50
                    }
                }
                val total = teacherCircles.size
                _resultText.value = "$correctCount / $total correct"

                Log.d("ResultViewModel", "Correct: $correctCount, Total: $total, Score: ${_resultText.value}")

                // Draw circles and overlay score on the student image
                val gradedMat = ImageProcessor.compareCircles(teacherCircles, studentCircles, studentMat)
                Imgproc.putText(
                    gradedMat, _resultText.value, Point(10.0, 50.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.2, org.opencv.core.Scalar(0.0, 0.0, 255.0), 3
                )

                val tmpBitmap = Bitmap.createBitmap(gradedMat.cols(), gradedMat.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(gradedMat, tmpBitmap)

                var finalName = "John Doe"
                var finalRoll = "12345"

                if (AppMode.isOnlineMode()) {
                    val apiResponse = ImageProcessor.extractStudentInfo(studentBitmap)
                    val candidateText = apiResponse.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text", "") ?: ""
                    val extractedName = ImageProcessor.extractField(candidateText, "Name:")
                    val extractedRoll = ImageProcessor.extractField(candidateText, "Roll No.:")
                    if (extractedName.isNotEmpty()) finalName = extractedName
                    if (extractedRoll.isNotEmpty()) finalRoll = extractedRoll
                }

                Log.d("StudentRecord", "Extracted Name: $finalName, Roll: $finalRoll, Score: ${_resultText.value}")

                withContext(Dispatchers.Main) {
                    _gradedBitmap.value = tmpBitmap
                    _isProcessing.value = false

                    if (!recordAdded) {
                        val existingRecordIndex = StudentRecord.getRecords().indexOfFirst { it.rollNumber == finalRoll }
                        if (existingRecordIndex != -1) {
                            StudentRecord.getRecords()[existingRecordIndex] =
                                StudentRecord(finalName, finalRoll, _resultText.value)
                        } else {
                            StudentRecord.addRecord(StudentRecord(finalName, finalRoll, _resultText.value))
                        }
                        recordAdded = true
                    }
                    onProcessingComplete()
                }
            } catch (e: Exception) {
                Log.e("ResultViewModel", "Error processing images", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onProcessingComplete()
                }
            }
        }
    }

    fun updateTeacherZoom(panX: Float, panY: Float, zoom: Float) {
        _scaleTeacher.value = (_scaleTeacher.value * zoom).coerceIn(1f, 3f)
        if (_scaleTeacher.value > 1f) {
            _offsetXTeacher.value = (_offsetXTeacher.value + panX).coerceIn(-200f, 200f)
            _offsetYTeacher.value = (_offsetYTeacher.value + panY).coerceIn(-200f, 200f)
        } else {
            _offsetXTeacher.value = 0f
            _offsetYTeacher.value = 0f
        }
    }

    fun updateGradedZoom(panX: Float, panY: Float, zoom: Float) {
        _scaleGraded.value = (_scaleGraded.value * zoom).coerceIn(1f, 3f)
        if (_scaleGraded.value > 1f) {
            _offsetXGraded.value = (_offsetXGraded.value + panX).coerceIn(-200f, 200f)
            _offsetYGraded.value = (_offsetYGraded.value + panY).coerceIn(-200f, 200f)
        } else {
            _offsetXGraded.value = 0f
            _offsetYGraded.value = 0f
        }
    }

    fun clearStudentImage(context: Context) {
        ImageCache.setStudentImage(context, null)
    }

    fun clearCache(context: Context) {
        ImageCache.clearCache(context)
        _teacherBitmap.value = null
    }

}

