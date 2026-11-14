package com.uniquemakeover.admin.feature.services.data.mapper

import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import com.uniquemakeover.admin.core.domain.model.Service
import org.junit.Test

class ServiceMapperTest {

    @Test
    fun toDomain_mapsAllEntityFieldsToDomainModel() {
        val entity = ServiceEntity(
            id = "s1",
            name = "Gold Facial",
            rate = 800,
            imagePath = "images/facial.jpg",
            sortOrder = 3
        )

        val domain = entity.toDomain()

        assertThat(domain.id).isEqualTo("s1")
        assertThat(domain.name).isEqualTo("Gold Facial")
        assertThat(domain.rate).isEqualTo(800)
        assertThat(domain.imagePath).isEqualTo("images/facial.jpg")
        assertThat(domain.sortOrder).isEqualTo(3)
    }

    @Test
    fun toEntity_mapsAllDomainFieldsToEntity() {
        val domain = Service(
            id = "s2",
            name = "Bridal Package",
            rate = 5000,
            imagePath = null,
            sortOrder = 7
        )

        val entity = domain.toEntity()

        assertThat(entity.id).isEqualTo("s2")
        assertThat(entity.name).isEqualTo("Bridal Package")
        assertThat(entity.rate).isEqualTo(5000)
        assertThat(entity.imagePath).isNull()
        assertThat(entity.sortOrder).isEqualTo(7)
    }
}
