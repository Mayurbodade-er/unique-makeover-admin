package com.uniquemakeover.admin.feature.reminders.domain.launcher

import com.uniquemakeover.admin.core.domain.result.DataResult

interface WhatsAppLauncher {
    fun launchReminder(phone: String, message: String): DataResult<Unit>
}
