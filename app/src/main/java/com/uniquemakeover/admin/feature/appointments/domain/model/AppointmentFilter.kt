package com.uniquemakeover.admin.feature.appointments.domain.model

enum class FilterTab {
    ALL,
    UPCOMING,
    COMPLETED,
    CANCELLED
}

data class DateRange(
    val startDate: String,
    val endDate: String
)

data class AppointmentFilter(
    val tab: FilterTab = FilterTab.UPCOMING,
    val dateRange: DateRange? = null,
    val searchQuery: String = ""
)
