package com.uniquemakeover.admin.feature.clients.data.mapper

import com.uniquemakeover.admin.core.database.model.ClientProfileAggregate
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientProfileMapperTest {

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val aggregate = ClientProfileAggregate(
            phone = "9822001122",
            name = "Neha Kulkarni",
            totalVisits = 4,
            totalSpend = 3200,
            lastVisitDate = "2026-11-20",
            lastServices = "Keratin Spa, Eyebrow",
            maxBookedAt = 1700000000000L
        )

        val domain = ClientProfileMapper.toDomain(aggregate)

        assertEquals("9822001122", domain.phone)
        assertEquals("Neha Kulkarni", domain.name)
        assertEquals(4, domain.totalVisits)
        assertEquals(3200, domain.totalSpend)
        assertEquals("2026-11-20", domain.lastVisitDate)
        assertEquals("Keratin Spa, Eyebrow", domain.lastServices)
    }

    @Test
    fun toDomain_handlesNullDatesAndServicesGracefully() {
        val aggregate = ClientProfileAggregate(
            phone = "9876543210",
            name = "Pooja Patil",
            totalVisits = 0,
            totalSpend = 0,
            lastVisitDate = null,
            lastServices = null,
            maxBookedAt = 0L
        )

        val domain = ClientProfileMapper.toDomain(aggregate)

        assertEquals("9876543210", domain.phone)
        assertEquals("", domain.lastVisitDate)
        assertEquals("", domain.lastServices)
    }
}
