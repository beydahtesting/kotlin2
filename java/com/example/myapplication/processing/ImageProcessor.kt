package com.example.myapplication.processing

import android.graphics.Bitmap
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.regex.Pattern
import kotlin.math.hypot

object ImageProcessor {
    private val GREEN = Scalar(0.0, 255.0, 0.0)
    private val RED = Scalar(0.0, 0.0, 255.0)
    private const val MATCH_THRESHOLD = 30.0

    // Process image: Ensure correct channel ordering then crop/warp & threshold.
    fun processImage(image: Mat): Mat {
        if (image.empty()) {
            Log.e("ImageProcessor", "Input image is null or empty")
            return image
        }
        var processedImage = image
        if (image.channels() == 4) {
            val bgr = Mat()
            Imgproc.cvtColor(image, bgr, Imgproc.COLOR_RGBA2BGR)
            processedImage = bgr
        } else if (image.channels() == 1) {
            val bgr = Mat()
            Imgproc.cvtColor(image, bgr, Imgproc.COLOR_GRAY2BGR)
            processedImage = bgr
        }

        val gray = Mat()
        Imgproc.cvtColor(processedImage, gray, Imgproc.COLOR_BGR2GRAY)
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            blurred, thresh, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0
        )

        val edges = Mat()
        Imgproc.Canny(thresh, edges, 50.0, 150.0)
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            edges,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        gray.release()
        blurred.release()
        edges.release()

        if (contours.isNotEmpty()) {
            var largestContour = contours[0]
            for (cnt in contours) {
                if (Imgproc.contourArea(cnt) > Imgproc.contourArea(largestContour)) {
                    largestContour = cnt
                }
            }
            val perimeter = Imgproc.arcLength(MatOfPoint2f(*largestContour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*largestContour.toArray()),
                approx,
                0.02 * perimeter,
                true
            )
            if (approx.total() == 4L) {
                val orderedPts = reorderPoints(approx)
                val width = 700.0
                val height = 800.0
                val dst = MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(width - 1, 0.0),
                    Point(width - 1, height - 1),
                    Point(0.0, height - 1)
                )
                val M = Imgproc.getPerspectiveTransform(orderedPts, dst)
                val warped = Mat()
                Imgproc.warpPerspective(processedImage, warped, M, Size(width, height))
                approx.release()
                orderedPts.release()
                M.release()
                dst.release()
                return warped
            }
        }
        return thresh // ✅ If no contours are found, return thresholded image
    }


    fun detectFilledCircles(image: Mat): List<Point> {
        val hsv = Mat()
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV)
        val lowerBlue = Scalar(90.0, 50.0, 50.0)
        val upperBlue = Scalar(130.0, 255.0, 255.0)
        val mask = Mat()
        Core.inRange(hsv, lowerBlue, upperBlue, mask)
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            mask,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        val filledCircles = ArrayList<Point>()
        for (cnt in contours) {
            val area = Imgproc.contourArea(cnt)
            if (area > 100 && area < 5000) {
                val center = Point()
                val radius = FloatArray(1)
                Imgproc.minEnclosingCircle(MatOfPoint2f(*cnt.toArray()), center, radius)
                filledCircles.add(center)
            }
        }
        hsv.release()
        mask.release()
        return filledCircles
    }

    fun compareCircles(teacherCircles: List<Point>, studentCircles: List<Point>, image: Mat): Mat {
        val correctMatches = mutableListOf<Point>()  // ✅ Tracks correct student answers
        val unmatchedStudent = studentCircles.toMutableList() // ✅ Copies student answers

        // ✅ Step 1: Match Student Answers to Teacher's Key
        for (teacherAnswer in teacherCircles) {
            val matchedStudent = unmatchedStudent.find { student ->
                hypot(teacherAnswer.x - student.x, teacherAnswer.y - student.y) < MATCH_THRESHOLD
            }

            if (matchedStudent != null) {
                correctMatches.add(matchedStudent)  // ✅ Mark as correct
                unmatchedStudent.remove(matchedStudent)  // ✅ Remove matched answer
            } else {
                // ❌ No matching student answer → **Draw empty green circle (missing answer)**
                Imgproc.circle(image, teacherAnswer, 10, GREEN, 2)
            }
        }

        // ✅ Step 2: Draw Correct Student Answers
        correctMatches.forEach { correctAnswer ->
            Imgproc.circle(image, correctAnswer, 10, GREEN, -1)  // ✅ Correct answers: **Filled Green**
        }

        // ❌ Step 3: Draw Extra (Wrong) Student Answers
        unmatchedStudent.forEach { wrongAnswer ->
            Imgproc.circle(image, wrongAnswer, 10, RED, -1)  // ❌ Wrong answers: **Filled Red**
        }

        // ✅ Convert Image Back to RGB (Final Step)
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB)
        return image
    }


    fun drawDetectedCircles(image: Mat): Mat {
        val circles = detectFilledCircles(image)
        val output = image.clone()
        for (p in circles) {
            Imgproc.circle(output, p, 10, GREEN, 2)
        }
        return output
    }

    private fun reorderPoints(points: MatOfPoint2f): MatOfPoint2f {
        val pts = points.toArray()
        if (pts.size != 4) return points
        val ordered = arrayOfNulls<Point>(4)
        val sums = DoubleArray(4)
        val diffs = DoubleArray(4)
        for (i in pts.indices) {
            sums[i] = pts[i].x + pts[i].y
            diffs[i] = pts[i].y - pts[i].x
        }
        var tl = 0
        var br = 0
        var tr = 0
        var bl = 0
        for (i in 1 until 4) {
            if (sums[i] < sums[tl]) tl = i
            if (sums[i] > sums[br]) br = i
            if (diffs[i] < diffs[tr]) tr = i
            if (diffs[i] > diffs[bl]) bl = i
        }
        ordered[0] = pts[tl]
        ordered[1] = pts[tr]
        ordered[2] = pts[br]
        ordered[3] = pts[bl]
        return MatOfPoint2f(*ordered.requireNoNulls())
    }

    // ✅ **Fix: Rotate Image Properly Using OpenCV**
    fun rotateImage(bitmap: Bitmap, angle: Int): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val rotatedMat = Mat()
        val center = org.opencv.core.Point(mat.cols() / 2.0, mat.rows() / 2.0)
        val rotationMatrix = Imgproc.getRotationMatrix2D(center, angle.toDouble(), 1.0)
        Imgproc.warpAffine(mat, rotatedMat, rotationMatrix, Size(mat.cols().toDouble(), mat.rows().toDouble()))

        val rotatedBitmap = Bitmap.createBitmap(rotatedMat.cols(), rotatedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rotatedMat, rotatedBitmap)
        return rotatedBitmap
    }

    // ✅ **Fix: Flip Image Horizontally or Vertically**
    fun flipImage(bitmap: Bitmap, flipCode: Int = 1): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val flippedMat = Mat()
        Core.flip(mat, flippedMat, flipCode)  // `1` = Horizontal, `0` = Vertical, `-1` = Both

        val flippedBitmap = Bitmap.createBitmap(flippedMat.cols(), flippedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(flippedMat, flippedBitmap)
        return flippedBitmap
    }

    fun extractField(text: String, label: String): String {
        val modifiedText = text.replace("\\n", "\n")
        val regex = Regex(Pattern.quote(label) + "\\s*(.*?)(\\n|\"|$)")
        return regex.find(modifiedText)?.groups?.get(1)?.value?.replace("*", "")?.trim() ?: ""
    }


    fun extractStudentInfo(bitmap: Bitmap): JSONObject {
        return try {
            // Compress image and encode as Base64 without newlines.
            val imageBytes = ImageUtils.compressToJPEG(bitmap, 30)
            val encodedImage =
                android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

            // Build the payload.
            val inlineData = JSONObject().apply {
                put("mime_type", "image/jpeg")
                put("data", encodedImage)
            }
            val partsArray = JSONArray().apply {
                put(JSONObject().put("inline_data", inlineData))
                put(
                    JSONObject().put(
                        "text",
                        "Extract only the student's name and roll number from this exam sheet image."
                    )
                )
            }
            val contentObject = JSONObject().apply { put("parts", partsArray) }
            val contentsArray = JSONArray().apply { put(contentObject) }
            val payload = JSONObject().apply { put("contents", contentsArray) }

            // Log the payload for debugging.
            android.util.Log.d("GeminiAPI", "Payload: $payload")

            // Create and open the connection.
            val url =
                URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDGalTcZxd_xWk1ZU6SQqgHl3KR5ZvKpoc")
            val conn = url.openConnection() as HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")

            // Send the payload.
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            // Check response code.
            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorResponse = conn.errorStream.bufferedReader().readText()
                android.util.Log.e(
                    "GeminiAPI",
                    "HTTP Error Code: $responseCode, Response: $errorResponse"
                )
                throw Exception("HTTP Error Code $responseCode")
            }

            // Read the successful response.
            val response = conn.inputStream.bufferedReader().readText()
            android.util.Log.d("GeminiAPI", "Response: $response")
            JSONObject(response)
        } catch (e: Exception) {
            android.util.Log.e("GeminiAPI", "Error extracting student info", e)
            // Return dummy values if the API call fails.
            JSONObject().apply {
                put("name", "John Doe")
                put("rollNumber", "12345")
            }
        }
    }
}
//val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDGalTcZxd_xWk1ZU6SQqgHl3KR5ZvKpoc")