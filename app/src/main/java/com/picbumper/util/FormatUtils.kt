package com.picbumper.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    /**
     * Formats file size in bytes into human-readable units (B, KB, MB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.2f MB", mb)
        } else if (kb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f KB", kb)
        } else {
            "$bytes B"
        }
    }

    /**
     * Formats epoch timestamp (seconds) into yyyy/MM/dd HH:mm:ss date string.
     */
    fun formatDateTime(timestampSec: Long, fallback: String = "未設定"): String {
        if (timestampSec <= 0) return fallback
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestampSec * 1000L))
    }
}
