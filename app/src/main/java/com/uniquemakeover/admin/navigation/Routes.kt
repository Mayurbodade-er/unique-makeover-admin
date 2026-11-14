package com.uniquemakeover.admin.navigation

object Routes {
    const val PinLock = "pin_lock"
    const val Dashboard = "dashboard"
    const val Appointments = "appointments"
    const val Services = "services"
    const val Clients = "clients"
    const val ClientDetail = "client_detail/{phone}"
    const val Reminders = "reminders"
    const val Settings = "settings"

    fun clientDetail(phone: String): String = "client_detail/$phone"
}
