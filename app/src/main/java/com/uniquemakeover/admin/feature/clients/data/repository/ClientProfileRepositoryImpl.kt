package com.uniquemakeover.admin.feature.clients.data.repository

import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.model.ClientProfileAggregate
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.clients.data.mapper.ClientProfileMapper
import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientProfileRepositoryImpl @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val phoneSanitizer: PhoneSanitizer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ClientProfileRepository {

    override fun getClientProfiles(): Flow<List<ClientProfile>> {
        return appointmentDao.observeClientProfiles()
            .map { aggregates -> mapAggregatesToProfiles(aggregates) }
            .flowOn(ioDispatcher)
    }

    override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return getClientProfiles()
        }
        return appointmentDao.searchClientProfiles(trimmed)
            .map { aggregates -> mapAggregatesToProfiles(aggregates) }
            .flowOn(ioDispatcher)
    }

    private fun mapAggregatesToProfiles(
        aggregates: List<ClientProfileAggregate>
    ): List<ClientProfile> {
        return aggregates
            .groupBy { aggregate ->
                phoneSanitizer.sanitize(aggregate.phone) ?: aggregate.phone
            }
            .map { (canonicalPhone, group) ->
                if (group.size == 1) {
                    ClientProfileMapper.toDomain(group.first(), canonicalPhone)
                } else {
                    mergeAggregates(canonicalPhone, group)
                }
            }
    }

    private fun mergeAggregates(
        canonicalPhone: String,
        group: List<ClientProfileAggregate>
    ): ClientProfile {
        val sortedByRecency = group.sortedByDescending { it.maxBookedAt }
        val latest = sortedByRecency.first()
        val maxVisitDate = group.mapNotNull { it.lastVisitDate }.maxOrNull().orEmpty()
        return ClientProfile(
            phone = canonicalPhone,
            name = latest.name,
            totalVisits = group.sumOf { it.totalVisits },
            totalSpend = group.sumOf { it.totalSpend },
            lastVisitDate = maxVisitDate,
            lastServices = latest.lastServices.orEmpty()
        )
    }
}
