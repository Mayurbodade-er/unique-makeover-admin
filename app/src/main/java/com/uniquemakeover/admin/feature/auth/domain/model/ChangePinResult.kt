package com.uniquemakeover.admin.feature.auth.domain.model

import com.uniquemakeover.admin.core.common.UiText

sealed interface ChangePinResult {
    data object Success : ChangePinResult
    data object IncorrectCurrentPin : ChangePinResult
    data object PinsDoNotMatch : ChangePinResult
    data object SameAsCurrentPin : ChangePinResult
    data class Error(val message: UiText) : ChangePinResult
}
