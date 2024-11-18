package com.example.mylibrary.util.extensions

import java.util.Calendar

fun Calendar.excludeTime() = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

fun Calendar.resetDayOfMonth() = apply {
    set(Calendar.DAY_OF_MONTH, 1)
}