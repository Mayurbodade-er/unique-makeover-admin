package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.settings.domain.model.BackupSnapshot
import com.uniquemakeover.admin.feature.settings.domain.model.toBackupDto
import com.uniquemakeover.admin.feature.settings.domain.repository.BackupWriter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository,
    private val appointmentRepository: AppointmentRepository,
    private val clientNoteRepository: ClientNoteRepository,
    private val backupWriter: BackupWriter,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(targetUriString: String): DataResult<Int> {
        val settings = settingsRepository.getSettingsSync() ?: return DataResult.Error(
            UiText.DynamicString("Unable to retrieve salon settings for backup")
        )
        val services = serviceRepository.getServices().first()
        val appointments = appointmentRepository.getAllAppointments().first()
        val notes = clientNoteRepository.getAllNotes()

        val timestamp = "${timeProvider.todayDate()}T${timeProvider.nowTime24()}:00Z"
        val snapshot = BackupSnapshot(
            version = BACKUP_SCHEMA_VERSION,
            exportedAt = timestamp,
            settings = settings.toBackupDto(),
            services = services.map { it.toBackupDto() },
            appointments = appointments.map { it.toBackupDto() },
            clientNotes = notes
        )

        val writeResult = backupWriter.writeBackup(targetUriString, snapshot)
        if (writeResult is DataResult.Success) {
            settingsRepository.updateLastBackup(timeProvider.todayDate())
        }
        return writeResult
    }

    companion object {
        const val BACKUP_SCHEMA_VERSION = 2
    }
}
