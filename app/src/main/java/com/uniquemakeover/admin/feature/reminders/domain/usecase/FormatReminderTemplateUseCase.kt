package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.model.TemplateTokens
import javax.inject.Inject

class FormatReminderTemplateUseCase @Inject constructor() {
    operator fun invoke(
        template: String,
        appointment: Appointment,
        settings: Settings
    ): String {
        val due = (appointment.rate - appointment.paid).coerceAtLeast(0)
        return template
            .replace(TemplateTokens.NAME, appointment.name)
            .replace(TemplateTokens.SHOP, settings.shop)
            .replace(TemplateTokens.SERVICE, appointment.serviceSnapshot)
            .replace(TemplateTokens.DATE, appointment.date)
            .replace(TemplateTokens.TIME, appointment.time)
            .replace(TemplateTokens.RATE, appointment.rate.toString())
            .replace(TemplateTokens.DUE, due.toString())
            .replace(TemplateTokens.SHOP_NUM, settings.wa)
    }
}
