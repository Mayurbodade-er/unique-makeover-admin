package com.uniquemakeover.admin.feature.clients.data.mapper

import com.uniquemakeover.admin.core.database.model.ClientProfileAggregate
import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile

object ClientProfileMapper {
    fun toDomain(
        aggregate: ClientProfileAggregate,
        canonicalPhone: String = aggregate.phone
    ): ClientProfile {
        return ClientProfile(
            phone = canonicalPhone,
            name = aggregate.name,
            totalVisits = aggregate.totalVisits,
            totalSpend = aggregate.totalSpend,
            lastVisitDate = aggregate.lastVisitDate.orEmpty(),
            lastServices = aggregate.lastServices.orEmpty()
        )
    }
}
