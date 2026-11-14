package com.uniquemakeover.admin.feature.appointments.data.mapper

import com.uniquemakeover.admin.core.database.entity.PaymentEntity
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.model.PaymentMode

object PaymentMapper {

    fun toDomain(entity: PaymentEntity): Payment {
        return Payment(
            id = entity.id,
            appointmentId = entity.appointmentId,
            amount = entity.amount,
            mode = PaymentMode.fromValue(entity.mode),
            paidAt = entity.paidAt
        )
    }

    fun toEntity(domain: Payment): PaymentEntity {
        return PaymentEntity(
            id = domain.id,
            appointmentId = domain.appointmentId,
            amount = domain.amount,
            mode = domain.mode.value,
            paidAt = domain.paidAt
        )
    }
}
