package com.example.mylibrary.util.extensions


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DateFormat(val format: String) {
    MMMM_YYYY("MMMM yyyy"),
    DD_MMM_YYYY("dd MMM yyyy"),
    EE("EE")
}

fun Long.toFormattedDateLabel(format: DateFormat) = Date(this).toFormattedDateLabel(format)

fun Date.toFormattedDateLabel(format: DateFormat): String {
    return runCatching {
        val formatter = SimpleDateFormat(format.format, Locale.getDefault())
        formatter.format(time)
    }.getOrNull().orEmpty()
}