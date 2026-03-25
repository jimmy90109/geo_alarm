package com.github.jimmy90109.geoalarm.appactions

sealed class AppActionResult<out T> {
    data class Success<T>(val value: T) : AppActionResult<T>()
    data class Error(val code: String, val message: String) : AppActionResult<Nothing>()
}
