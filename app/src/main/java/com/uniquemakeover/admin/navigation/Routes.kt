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
    const val UpiQr = "upi_qr?amount={amount}&clientName={clientName}"

    fun clientDetail(phone: String): String = "client_detail/$phone"
    fun upiQr(amount: Int = 0, clientName: String = ""): String = "upi_qr?amount=$amount&clientName=$clientName"
}
