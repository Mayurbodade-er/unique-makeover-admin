package com.uniquemakeover.admin.feature.reminders.data.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.reminders.domain.launcher.WhatsAppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phoneSanitizer: PhoneSanitizer
) : WhatsAppLauncher {

    override fun launchReminder(phone: String, message: String): DataResult<Unit> {
        val sanitizedPhone = phoneSanitizer.sanitize(phone)
            ?: return DataResult.Error(
                UiText.DynamicString("Invalid phone number. Please edit appointment to correct")
            )

        val fullPhoneNumber = if (sanitizedPhone.startsWith("91")) {
            sanitizedPhone
        } else {
            "91$sanitizedPhone"
        }

        val encodedMessage = try {
            URLEncoder.encode(message, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        } catch (_: Exception) {
            Uri.encode(message)
        }

        val uri = Uri.parse("https://wa.me/$fullPhoneNumber?text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            DataResult.Success(Unit)
        } catch (_: ActivityNotFoundException) {
            DataResult.Error(
                UiText.DynamicString("WhatsApp is not installed on this device")
            )
        } catch (e: SecurityException) {
            DataResult.Error(
                UiText.DynamicString(e.localizedMessage ?: "Failed to open WhatsApp"),
                e
            )
        }
    }
}
