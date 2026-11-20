package com.uniquemakeover.admin.feature.upi.domain.launcher

import com.uniquemakeover.admin.core.domain.result.DataResult

interface UpiWhatsAppLauncher {
    fun launchPaymentRequest(phone: String, message: String): DataResult<Unit>
}
