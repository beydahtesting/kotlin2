package com.example.myapplication.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageCache {
    private const val TEACHER_IMAGE_FILENAME = "teacher_image.png"
    private const val STUDENT_IMAGE_FILENAME = "student_image.png"

    // **🟢 Save image to storage (Now Handles Null Bitmap Properly)**
    private fun saveImage(context: Context, bitmap: Bitmap?, filename: String) {
        if (bitmap == null) {
            Log.e("ImageCache", "❌ Cannot save null image: $filename")
            return
        }

        try {
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("ImageCache", "✅ Image saved successfully: $filename")
        } catch (e: Exception) {
            Log.e("ImageCache", "❌ Error saving image: $filename", e)
        }
    }

    // **🟢 Load image from storage (Now Prevents Null Issues)**
    private fun loadImage(context: Context, filename: String): Bitmap? {
        return try {
            val file = File(context.cacheDir, filename)
            if (!file.exists()) {
                Log.e("ImageCache", "❌ Image not found: $filename")
                return null
            }

            BitmapFactory.decodeFile(file.absolutePath)?.also {
                Log.d("ImageCache", "✅ Loaded image from cache: $filename")
            }
        } catch (e: Exception) {
            Log.e("ImageCache", "❌ Error loading image: $filename", e)
            null
        }
    }

    // **🟢 Set & Save Teacher Image (Fix: Ensures Bitmap is Not Null)**
    fun setTeacherImage(context: Context, image: Bitmap?) {
        image?.let { saveImage(context, it, TEACHER_IMAGE_FILENAME) }
    }

    // **🟢 Get Teacher Image**
    fun getTeacherImage(context: Context): Bitmap? {
        return loadImage(context, TEACHER_IMAGE_FILENAME)
    }

    // **🟢 Set & Save Student Image (Fix: Ensures Bitmap is Not Null)**
    fun setStudentImage(context: Context, image: Bitmap?) {
        image?.let { saveImage(context, it, STUDENT_IMAGE_FILENAME) }
    }

    // **🟢 Get Student Image**
    fun getStudentImage(context: Context): Bitmap? {
        return loadImage(context, STUDENT_IMAGE_FILENAME)
    }

    // **🗑️ Clear Cache (Fix: Ensures Proper File Deletion)**
    fun clearCache(context: Context) {
        val teacherFile = File(context.cacheDir, TEACHER_IMAGE_FILENAME)
        val studentFile = File(context.cacheDir, STUDENT_IMAGE_FILENAME)

        if (teacherFile.exists() && teacherFile.delete()) {
            Log.d("ImageCache", "🗑️ Teacher Image Deleted")
        } else {
            Log.e("ImageCache", "❌ Failed to Delete Teacher Image")
        }

        if (studentFile.exists() && studentFile.delete()) {
            Log.d("ImageCache", "🗑️ Student Image Deleted")
        } else {
            Log.e("ImageCache", "❌ Failed to Delete Student Image")
        }
    }
}
