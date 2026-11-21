package com.uniquemakeover.admin.feature.dashboard.domain.model

sealed interface BannerDeepLinkTarget {
    data object SettingsComms : BannerDeepLinkTarget
    data object SettingsData : BannerDeepLinkTarget
    data object AppointmentsOverdue : BannerDeepLinkTarget
    data object RemindersTomorrow : BannerDeepLinkTarget
}

sealed interface AlertBanner {
    val id: String
    val deepLinkTarget: BannerDeepLinkTarget
    val priority: Int

    data object BackupOverdueMissing : AlertBanner {
        override val id: String = "backup_overdue"
        override val deepLinkTarget: BannerDeepLinkTarget = BannerDeepLinkTarget.SettingsData
        override val priority: Int = 1
    }

    data class BackupOverdue(val daysSinceLast: Int) : AlertBanner {
        override val id: String = "backup_overdue"
        override val deepLinkTarget: BannerDeepLinkTarget = BannerDeepLinkTarget.SettingsData
        override val priority: Int = 1
    }

    data class RemindersPending(val count: Int) : AlertBanner {
        override val id: String = "reminders_pending"
        override val deepLinkTarget: BannerDeepLinkTarget = BannerDeepLinkTarget.RemindersTomorrow
        override val priority: Int = 2
    }

    data object UpiNotConfigured : AlertBanner {
        override val id: String = "upi_not_configured"
        override val deepLinkTarget: BannerDeepLinkTarget = BannerDeepLinkTarget.SettingsComms
        override val priority: Int = 3
    }

    data object WaNotConfigured : AlertBanner {
        override val id: String = "wa_not_configured"
        override val deepLinkTarget: BannerDeepLinkTarget = BannerDeepLinkTarget.SettingsComms
        override val priority: Int = 4
    }

    data class OverdueAppointments(val count: Int) : AlertBanner {
        override val id: String = "overdue_appointments"
        override val deepLinkTarget: BannerDeepLinkTarget = BannerDeepLinkTarget.AppointmentsOverdue
        override val priority: Int = 5
    }
}
