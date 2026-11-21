package com.uniquemakeover.admin.feature.dashboard.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val OVERDUE_BACKUP_DAYS_THRESHOLD = 3
private const val DATE_STRING_LENGTH = 10

class GetAlertBannersUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<List<AlertBanner>> {
        return combine(
            settingsRepository.getSettings(),
            appointmentRepository.getAllAppointments()
        ) { settings, appointments ->
            evaluateAlertBanners(settings, appointments)
        }
    }

    private fun evaluateAlertBanners(
        settings: Settings?,
        appointments: List<Appointment>
    ): List<AlertBanner> {
        val todayStr = timeProvider.todayDate()
        val tomorrowStr = timeProvider.tomorrowDate()
        val todayDate = runCatching { LocalDate.parse(todayStr) }.getOrNull() ?: LocalDate.now()

        val banners = mutableListOf<AlertBanner>()

        evaluateBackupAlert(settings, todayDate)?.let { banners.add(it) }

        val pendingRemindersCount = appointments.count { apt ->
            apt.date == tomorrowStr &&
                apt.status.equals(AppointmentStatus.BOOKED.value, ignoreCase = true) &&
                (apt.reminded.isNullOrBlank() || apt.reminded != tomorrowStr)
        }
        if (pendingRemindersCount > 0) {
            banners.add(AlertBanner.RemindersPending(pendingRemindersCount))
        }

        if (settings == null || settings.upi.isBlank()) {
            banners.add(AlertBanner.UpiNotConfigured)
        }

        if (settings == null || settings.wa.isBlank()) {
            banners.add(AlertBanner.WaNotConfigured)
        }

        val overdueAppointmentsCount = appointments.count { apt ->
            apt.date < todayStr && apt.status.equals(AppointmentStatus.BOOKED.value, ignoreCase = true)
        }
        if (overdueAppointmentsCount > 0) {
            banners.add(AlertBanner.OverdueAppointments(overdueAppointmentsCount))
        }

        return banners.sortedBy { it.priority }
    }

    private fun evaluateBackupAlert(settings: Settings?, today: LocalDate): AlertBanner? {
        val lastBackup = settings?.lastBackup
        val backupDate = if (!lastBackup.isNullOrBlank()) {
            runCatching { LocalDate.parse(lastBackup.take(DATE_STRING_LENGTH)) }.getOrNull()
        } else {
            null
        }

        return when {
            backupDate == null -> AlertBanner.BackupOverdueMissing
            ChronoUnit.DAYS.between(backupDate, today).toInt() > OVERDUE_BACKUP_DAYS_THRESHOLD -> {
                AlertBanner.BackupOverdue(ChronoUnit.DAYS.between(backupDate, today).toInt())
            }
            else -> null
        }
    }
}
