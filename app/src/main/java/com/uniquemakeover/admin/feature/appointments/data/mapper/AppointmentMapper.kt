package com.uniquemakeover.admin.feature.appointments.data.mapper

import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment

object AppointmentMapper {

    fun toDomain(entity: AppointmentEntity): Appointment = Appointment(
        id = entity.id,
        name = entity.name,
        phone = entity.phone,
        serviceSnapshot = entity.svc,
        serviceIds = entity.serviceIds,
        rate = entity.rate,
        paid = entity.paid,
        staff = entity.staff,
        date = entity.date,
        time = entity.time,
        note = entity.note,
        status = entity.status,
        reminded = entity.reminded,
        bookedAt = entity.bookedAt
    )

    fun toEntity(domain: Appointment): AppointmentEntity = AppointmentEntity(
        id = domain.id,
        name = domain.name,
        phone = domain.phone,
        svc = domain.serviceSnapshot,
        serviceIds = domain.serviceIds,
        rate = domain.rate,
        paid = domain.paid,
        staff = domain.staff,
        date = domain.date,
        time = domain.time,
        note = domain.note,
        status = domain.status,
        reminded = domain.reminded,
        bookedAt = domain.bookedAt
    )
}
