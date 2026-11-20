package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.reminders.domain.launcher.WhatsAppLauncher
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.model.TemplateTokens
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import javax.inject.Inject

class LaunchWhatsAppReminderUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
    private val formatReminderTemplateUseCase: FormatReminderTemplateUseCase,
    private val whatsAppLauncher: WhatsAppLauncher
) {
    suspend operator fun invoke(item: ReminderQueueItem, tomorrowDate: String): DataResult<Unit> {
        val rawTemplate = settingsRepository.getReminderTemplate()
        val template = if (rawTemplate.isNotBlank()) rawTemplate else TemplateTokens.DEFAULT_TEMPLATE
        val settings = settingsRepository.getSettingsSync() ?: Settings()

        val formattedMessage = formatReminderTemplateUseCase(
            template = template,
            appointment = item.appointment,
            settings = settings
        )

        val launchResult = whatsAppLauncher.launchReminder(
            phone = item.appointment.phone,
            message = formattedMessage
        )

        if (launchResult is DataResult.Success) {
            return remindersRepository.markReminded(item.appointment.id, tomorrowDate)
        }
        return launchResult
    }
}
