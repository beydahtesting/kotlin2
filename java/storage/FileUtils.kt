package com.example.myapplication.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.myapplication.models.StudentRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {

    // ✅ **Save Student Records as Excel (.xlsx)**
    suspend fun saveExcelFile(context: Context, studentRecords: List<StudentRecord>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Student Records")

                // ✅ **Create Header Row**
                val headerRow: Row = sheet.createRow(0)
                headerRow.createCell(0, CellType.STRING).setCellValue("Name")
                headerRow.createCell(1, CellType.STRING).setCellValue("Roll Number")
                headerRow.createCell(2, CellType.STRING).setCellValue("Score")

                // ✅ **Insert Data Rows**
                for ((index, record) in studentRecords.withIndex()) {
                    val row: Row = sheet.createRow(index + 1)
                    row.createCell(0, CellType.STRING).setCellValue(record.name)
                    row.createCell(1, CellType.STRING).setCellValue(record.rollNumber)
                    row.createCell(2, CellType.STRING).setCellValue(record.score)
                }

                // ✅ **Create a Unique File**
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(dir, "StudentRecords_${System.currentTimeMillis()}.xlsx")

                // ✅ **Save the Excel File**
                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                    workbook.close()
                }

                // ✅ **Show Success Message**
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Excel saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error Saving Excel", Toast.LENGTH_LONG).show()
                }
                false
            }
        }
    }

    // ✅ **Save Student Records as PDF (.pdf) with Table Formatting**
    suspend fun savePdfFile(context: Context, studentRecords: List<StudentRecord>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(600, 1000, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val paint = Paint()

                // ✅ **Title Header**
                paint.textSize = 20f
                paint.isFakeBoldText = true
                canvas.drawText("Student Records", 200f, 50f, paint)

                // ✅ **Table Header Formatting**
                paint.textSize = 16f
                paint.isFakeBoldText = true
                val startX = 50f
                val columnWidths = listOf(250f, 150f, 150f) // Adjust column widths
                val headerY = 100f

                canvas.drawText("Name", startX, headerY, paint)
                canvas.drawText("Roll Number", startX + columnWidths[0], headerY, paint)
                canvas.drawText("Score", startX + columnWidths[0] + columnWidths[1], headerY, paint)

                // ✅ **Line Under Header**
                canvas.drawLine(50f, 110f, 550f, 110f, paint)

                // ✅ **Write Data in Table Format**
                paint.textSize = 14f
                paint.isFakeBoldText = false
                var yPosition = 140f

                for (record in studentRecords) {
                    canvas.drawText(record.name, startX, yPosition, paint)
                    canvas.drawText(record.rollNumber, startX + columnWidths[0], yPosition, paint)
                    canvas.drawText(record.score, startX + columnWidths[0] + columnWidths[1], yPosition, paint)
                    yPosition += 30f

                    // ✅ **Prevent Content Overflow (Next Page)**
                    if (yPosition > 950f) {
                        pdfDocument.finishPage(page)
                        yPosition = 140f
                    }
                }

                pdfDocument.finishPage(page)

                // ✅ **Create a Unique File**
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(dir, "StudentRecords_${System.currentTimeMillis()}.pdf")

                // ✅ **Save the PDF File**
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()

                // ✅ **Show Success Message**
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error Saving PDF", Toast.LENGTH_LONG).show()
                }
                false
            }
        }
    }
}
