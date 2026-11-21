package com.uniquemakeover.admin.feature.settings.domain.model

data class BrandingConfig(
    val shopName: String = "Unique Makeover",
    val tagline: String = "Luxury Beauty Parlour",
    val logoPath: String? = null,
    val wallpaperPath: String? = null
)
