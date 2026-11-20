package com.uniquemakeover.admin.feature.reminders.data.platform

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class WhatsAppLauncherTest {

    private lateinit var application: Application
    private lateinit var launcher: WhatsAppLauncherImpl

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        launcher = WhatsAppLauncherImpl(application, PhoneSanitizer())
    }

    @Test
    fun `launches implicit intent targeting wa me with 91 prefix and encoded message`() {
        val result = launcher.launchReminder(
            phone = "9822112233",
            message = "Hello Priya! Your facial is tomorrow. Total: ₹800. ✨"
        )

        assertTrue(result is DataResult.Success)

        val nextIntent = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertTrue(nextIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)

        val dataString = nextIntent.dataString ?: ""
        assertTrue(dataString.startsWith("https://wa.me/919822112233?text="))
        assertTrue(dataString.contains("Hello%20Priya"))
        assertTrue(dataString.contains("%E2%9C%A8") || dataString.contains("✨"))
    }

    @Test
    fun `rejects invalid phone numbers without launching intent`() {
        val result = launcher.launchReminder(
            phone = "12345",
            message = "Hello!"
        )

        assertTrue(result is DataResult.Error)
        assertEquals(null, shadowOf(application).nextStartedActivity)
    }
}
