package com.uniquemakeover.admin.feature.upi.data.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.upi.domain.launcher.UpiWhatsAppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpiWhatsAppLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phoneSanitizer: PhoneSanitizer
) : UpiWhatsAppLauncher {

    override fun launchPaymentRequest(phone: String, message: String): DataResult<Unit> {
        val encodedMessage = try {
            URLEncoder.encode(message, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        } catch (_: Exception) {
            Uri.encode(message)
        }

        val uriString = if (phone.isNotBlank()) {
            val sanitized = phoneSanitizer.sanitize(phone)
            val fullPhone = if (sanitized != null) {
                if (sanitized.startsWith("91")) sanitized else "91$sanitized"
            } else {
                phone.filter { it.isDigit() }
            }
            "https://wa.me/$fullPhone?text=$encodedMessage"
        } else {
            "https://wa.me/?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            DataResult.Success(Unit)
        } catch (_: ActivityNotFoundException) {
            DataResult.Error(UiText.DynamicString("WhatsApp is not installed on this device"))
        } catch (e: SecurityException) {
            DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to open WhatsApp"), e)
        }
    }
}
