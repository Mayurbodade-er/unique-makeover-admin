package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.clients.domain.model.ClientHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetClientHistoryUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    companion object {
        private const val PHONE_DIGITS_LENGTH = 10
    }

    operator fun invoke(phone: String): Flow<List<ClientHistoryItem>> {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "").takeLast(PHONE_DIGITS_LENGTH)
        return appointmentRepository.getAppointmentsByPhone(cleanPhone).map { appointments ->
            appointments
                .sortedWith(
                    compareByDescending<com.uniquemakeover.admin.feature.appointments.domain.model.Appointment> {
                        it.date
                    }.thenByDescending { it.time }
                )
                .map { apt ->
                    val status = AppointmentStatus.fromValue(apt.status)
                    val balance = if (status == AppointmentStatus.CANCEL) {
                        0
                    } else {
                        maxOf(0, apt.rate - apt.paid)
                    }
                    ClientHistoryItem(
                        appointmentId = apt.id,
                        date = apt.date,
                        time = apt.time,
                        formattedServices = apt.serviceSnapshot,
                        staff = apt.staff,
                        rate = apt.rate,
                        paid = apt.paid,
                        balanceDue = balance,
                        status = status
                    )
                }
        }
    }
}
