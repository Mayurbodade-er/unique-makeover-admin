package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BuildRebookDraftUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val serviceRepository: ServiceRepository
) {
    companion object {
        private const val PHONE_DIGITS_LENGTH = 10
    }

    suspend operator fun invoke(phone: String): DataResult<BookingDraft> {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "").takeLast(PHONE_DIGITS_LENGTH)
        val appointments = appointmentRepository.getAppointmentsByPhone(cleanPhone).first()
        val nonCancelled = appointments.filter { it.status.lowercase() != "cancel" }

        val draft = if (nonCancelled.isNotEmpty()) {
            val latest = nonCancelled.maxWithOrNull(
                compareBy<Appointment> { it.date }.thenBy { it.time }
            ) ?: nonCancelled.first()

            val catalogServices = serviceRepository.getServices().first()
            val matchedServices = resolveMatchedServices(catalogServices, latest)

            BookingDraft(
                name = latest.name,
                phone = cleanPhone,
                selectedServices = matchedServices
            )
        } else {
            val clientName = appointments.firstOrNull()?.name.orEmpty()
            BookingDraft(
                name = clientName,
                phone = cleanPhone,
                selectedServices = emptyList()
            )
        }

        return DataResult.Success(draft)
    }

    private fun resolveMatchedServices(
        catalogServices: List<Service>,
        appointment: Appointment
    ): List<Service> {
        val byId = catalogServices.filter { appointment.serviceIds.contains(it.id) }
        if (byId.isNotEmpty()) return byId

        val snapshotNames = appointment.serviceSnapshot
            .split("+", ",")
            .map { it.trim().lowercase() }
        return catalogServices.filter { snapshotNames.contains(it.name.lowercase()) }
    }
}
