package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import com.uniquemakeover.admin.feature.dashboard.domain.model.BannerDeepLinkTarget
import com.uniquemakeover.admin.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertBannerDeepLinkTest {

    @Test
    fun alertBanners_mapToExpectedDeepLinkTargets() {
        assertEquals(BannerDeepLinkTarget.SettingsComms, AlertBanner.WaNotConfigured.deepLinkTarget)
        assertEquals(BannerDeepLinkTarget.SettingsComms, AlertBanner.UpiNotConfigured.deepLinkTarget)
        assertEquals(BannerDeepLinkTarget.SettingsData, AlertBanner.BackupOverdueMissing.deepLinkTarget)
        assertEquals(BannerDeepLinkTarget.SettingsData, AlertBanner.BackupOverdue(5).deepLinkTarget)
        assertEquals(BannerDeepLinkTarget.AppointmentsOverdue, AlertBanner.OverdueAppointments(3).deepLinkTarget)
        assertEquals(BannerDeepLinkTarget.RemindersTomorrow, AlertBanner.RemindersPending(2).deepLinkTarget)
    }

    @Test
    fun deepLinkTargets_resolveToExpectedRoutes() {
        val targets = listOf(
            BannerDeepLinkTarget.SettingsComms to Routes.Settings,
            BannerDeepLinkTarget.SettingsData to Routes.Settings,
            BannerDeepLinkTarget.AppointmentsOverdue to Routes.Appointments,
            BannerDeepLinkTarget.RemindersTomorrow to Routes.Reminders
        )

        for ((target, expectedRoute) in targets) {
            val route = resolveRouteForTarget(target)
            assertEquals(expectedRoute, route)
        }
    }

    private fun resolveRouteForTarget(target: BannerDeepLinkTarget): String {
        return when (target) {
            BannerDeepLinkTarget.SettingsComms,
            BannerDeepLinkTarget.SettingsData -> Routes.Settings
            BannerDeepLinkTarget.AppointmentsOverdue -> Routes.Appointments
            BannerDeepLinkTarget.RemindersTomorrow -> Routes.Reminders
        }
    }
}
