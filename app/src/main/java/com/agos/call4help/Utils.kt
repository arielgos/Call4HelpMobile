package com.agos.call4help

import android.annotation.SuppressLint
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {
        const val tag = "Call4Help"
        const val dateFormat = "dd/MM/yyyy HH:mm"
        const val imageWith = 640
        const val imageHeight = 480
        const val imageUrl = "https://firebasestorage.googleapis.com/v0/b/call4help-a06cf.appspot.com/o/"
        const val requestNewImage = 10000
        const val requestNewAlert = 2000
    }
}

/**
 * Extensions
 */

fun Bitmap.createFile(path: String): File {
    val file = File(path)
    val bos = ByteArrayOutputStream();
    this.compress(Bitmap.CompressFormat.JPEG, 100, bos)
    val bitmapData = bos.toByteArray()
    val fos = FileOutputStream(file)
    fos.write(bitmapData)
    fos.flush()
    fos.close()
    return file
}

fun String?.toIntOrDefault(default: Int = 0): Int {
    return this?.toIntOrNull() ?: default
}

@SuppressLint("SimpleDateFormat")
fun Date.toFormattedString(format: String = "dd/MM/yyyy HH:mm"): String {
    return SimpleDateFormat(format).format(this)
}

fun Double.format(digits: Int = 2) = "%.${digits}f".format(this)
