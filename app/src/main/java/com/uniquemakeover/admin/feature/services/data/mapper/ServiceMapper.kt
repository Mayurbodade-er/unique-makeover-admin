package com.uniquemakeover.admin.feature.services.data.mapper

import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import com.uniquemakeover.admin.core.domain.model.Service

fun ServiceEntity.toDomain(): Service {
    return Service(
        id = id,
        name = name,
        rate = rate,
        imagePath = imagePath,
        sortOrder = sortOrder
    )
}

fun Service.toEntity(): ServiceEntity {
    return ServiceEntity(
        id = id,
        name = name,
        rate = rate,
        imagePath = imagePath,
        sortOrder = sortOrder
    )
}
