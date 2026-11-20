package com.uniquemakeover.admin.navigation

data class UpiQrArgs(
    val amount: Int = 0,
    val rate: Int = 0,
    val paid: Int = 0,
    val clientName: String = "",
    val phone: String = "",
    val services: String = ""
)

object Routes {
    const val PinLock = "pin_lock"
    const val Dashboard = "dashboard"
    const val Appointments = "appointments"
    const val Services = "services"
    const val Clients = "clients"
    const val ClientDetail = "client_detail/{phone}"
    const val Reminders = "reminders"
    const val Settings = "settings"
    const val UpiQr =
        "upi_qr?amount={amount}&rate={rate}&paid={paid}&clientName={clientName}&phone={phone}&services={services}"

    fun clientDetail(phone: String): String = "client_detail/$phone"
    fun upiQr(args: UpiQrArgs = UpiQrArgs()): String =
        "upi_qr?amount=${args.amount}&rate=${args.rate}&paid=${args.paid}" +
            "&clientName=${args.clientName}&phone=${args.phone}&services=${args.services}"
}
