package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun saveReceiptImage(context: Context, uri: Uri): String? {
        return try {
            val receiptsDir = File(context.filesDir, "receipts")
            if (!receiptsDir.exists()) {
                receiptsDir.mkdirs()
            }
            val fileName = "nota_${System.currentTimeMillis()}.jpg"
            val destFile = File(receiptsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteReceiptImage(path: String?) {
        if (!path.isNullOrBlank()) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getReceiptsFolderSize(context: Context): Pair<Int, Long> {
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) return Pair(0, 0L)
        val files = receiptsDir.listFiles() ?: return Pair(0, 0L)
        val count = files.size
        val totalBytes = files.sumOf { it.length() }
        return Pair(count, totalBytes)
    }
}

object CurrencyUtils {
    private val indonesianLocale = Locale("in", "ID")
    private val numberFormat = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return try {
            numberFormat.format(amount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp $amount"
        }
    }

    fun formatNumber(amount: Long): String {
        return try {
            val df = NumberFormat.getNumberInstance(indonesianLocale)
            df.format(amount)
        } catch (e: Exception) {
            amount.toString()
        }
    }
}

object DateUtils {
    private val indonesianLocale = Locale("id", "ID")

    fun formatFullDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateShort(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }
}
