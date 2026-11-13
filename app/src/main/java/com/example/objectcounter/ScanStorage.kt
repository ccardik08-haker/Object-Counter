package com.example.objectcounter

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

data class SavedScan(
    val imageFile: File,
    val objectSummary: String
)

object ScanStorage {
    private fun getScansDir(context: Context): File {
        val dir = File(context.filesDir, "scans")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveScan(context: Context, bitmap: Bitmap, objectsText: String) {
        val timestamp = System.currentTimeMillis()
        val imageFile = File(getScansDir(context), "scan_$timestamp.jpg")
        val textFile = File(getScansDir(context), "scan_$timestamp.txt")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        textFile.writeText(objectsText)
    }

    fun loadScans(context: Context): List<SavedScan> {
        val dir = getScansDir(context)
        val scans = mutableListOf<SavedScan>()
        dir.listFiles()?.filter { it.extension == "jpg" }?.forEach { imageFile ->
            val textFile = File(dir, "${imageFile.nameWithoutExtension}.txt")
            val text = if (textFile.exists()) textFile.readText() else "Нет данных"
            scans.add(SavedScan(imageFile, text))
        }
        return scans.sortedByDescending { it.imageFile.lastModified() }
    }

    fun deleteScan(scan: SavedScan) {
        if (scan.imageFile.exists()) scan.imageFile.delete()
        val textFile = File(scan.imageFile.parentFile, "${scan.imageFile.nameWithoutExtension}.txt")
        if (textFile.exists()) textFile.delete()
    }
}