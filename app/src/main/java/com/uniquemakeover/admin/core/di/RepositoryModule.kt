package com.uniquemakeover.admin.core.di

import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.feature.appointments.data.repository.AppointmentRepositoryImpl
import com.uniquemakeover.admin.feature.appointments.data.repository.PaymentRepositoryImpl
import com.uniquemakeover.admin.feature.appointments.data.repository.SlotRepositoryImpl
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.feature.auth.data.PinRepositoryImpl
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import com.uniquemakeover.admin.feature.clients.data.repository.ClientNoteRepositoryImpl
import com.uniquemakeover.admin.feature.clients.data.repository.ClientProfileRepositoryImpl
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import com.uniquemakeover.admin.feature.reminders.data.platform.WhatsAppLauncherImpl
import com.uniquemakeover.admin.feature.reminders.data.repository.RemindersRepositoryImpl
import com.uniquemakeover.admin.feature.reminders.domain.launcher.WhatsAppLauncher
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import com.uniquemakeover.admin.feature.services.data.image.LocalImageStore
import com.uniquemakeover.admin.feature.services.data.repository.ServiceRepositoryImpl
import com.uniquemakeover.admin.feature.services.domain.repository.ImageStorageRepository
import com.uniquemakeover.admin.feature.settings.data.repository.SettingsRepositoryImpl
import com.uniquemakeover.admin.feature.upi.data.platform.ZxingQrEncoder
import com.uniquemakeover.admin.feature.upi.domain.repository.QrRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindImageStorageRepository(
        localImageStore: LocalImageStore
    ): ImageStorageRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ): ServiceRepository

    @Binds
    @Singleton
    abstract fun bindPinRepository(
        pinRepositoryImpl: PinRepositoryImpl
    ): PinRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSlotRepository(
        slotRepositoryImpl: SlotRepositoryImpl
    ): SlotRepository

    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(
        appointmentRepositoryImpl: AppointmentRepositoryImpl
    ): AppointmentRepository

    @Binds
    @Singleton
    abstract fun bindClientProfileRepository(
        clientProfileRepositoryImpl: ClientProfileRepositoryImpl
    ): ClientProfileRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindClientNoteRepository(
        clientNoteRepositoryImpl: ClientNoteRepositoryImpl
    ): ClientNoteRepository

    @Binds
    @Singleton
    abstract fun bindRemindersRepository(
        remindersRepositoryImpl: RemindersRepositoryImpl
    ): RemindersRepository

    @Binds
    @Singleton
    abstract fun bindWhatsAppLauncher(
        whatsAppLauncherImpl: WhatsAppLauncherImpl
    ): WhatsAppLauncher

    @Binds
    @Singleton
    abstract fun bindQrRepository(
        zxingQrEncoder: ZxingQrEncoder
    ): QrRepository
}
