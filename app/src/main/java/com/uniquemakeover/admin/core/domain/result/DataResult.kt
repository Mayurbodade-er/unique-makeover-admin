package com.uniquemakeover.admin.core.domain.result

import com.uniquemakeover.admin.core.common.UiText

sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Error(val message: UiText, val cause: Throwable? = null) : DataResult<Nothing>
}
