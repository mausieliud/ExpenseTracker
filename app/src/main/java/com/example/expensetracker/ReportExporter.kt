package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ReportExporter {

    /**
     * Export report data to a file and return the URI
     */
    fun exportReportToFile(context: Context, reportText: String, isCSV: Boolean): Uri? {
        try {
            // Create file name based on timestamp
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = if (isCSV) "budget_report_$timestamp.csv" else "budget_summary_$timestamp.txt"

            // Get the directory for storing files
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir

            // Create reports directory if it doesn't exist
            val reportsDir = File(documentsDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            // Create the file
            val reportFile = File(reportsDir, fileName)

            // Write content to the file
            FileOutputStream(reportFile).use { outputStream ->
                outputStream.write(reportText.toByteArray())
            }

            // Return content URI using FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Share the exported report file
     */
    fun shareReport(context: Context, fileUri: Uri, isCSV: Boolean) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (isCSV) "text/csv" else "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Add subject and text for better sharing experience
            putExtra(Intent.EXTRA_SUBJECT, "Budget Tracker Report")
            putExtra(Intent.EXTRA_TEXT, "Here's my budget report from Budget Tracker app.")
        }

        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}