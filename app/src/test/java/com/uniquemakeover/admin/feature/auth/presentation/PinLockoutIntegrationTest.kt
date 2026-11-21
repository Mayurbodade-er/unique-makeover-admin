package com.uniquemakeover.admin.feature.auth.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.security.LockoutPolicy
import com.uniquemakeover.admin.core.security.PinHasher
import com.uniquemakeover.admin.core.security.PinLockManager
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.auth.data.PinRepositoryImpl
import com.uniquemakeover.admin.feature.auth.domain.usecase.GetLockoutStateUseCase
import com.uniquemakeover.admin.feature.auth.domain.usecase.VerifyPinUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

private const val TIMEOUT_MILLIS = 3000L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PinLockoutIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var pinLockManager: PinLockManager
    private lateinit var pinRepository: PinRepositoryImpl
    private lateinit var verifyPinUseCase: VerifyPinUseCase
    private lateinit var getLockoutStateUseCase: GetLockoutStateUseCase
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var viewModel: PinLockViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val pinHasher = PinHasher()
        val lockoutPolicy = LockoutPolicy()
        fakeTimeProvider = FakeTimeProvider(currentElapsedRealtime = 10000L)

        val uniqueName = "int_test_auth_prefs_${UUID.randomUUID()}"
        val testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(uniqueName) }
        )

        pinLockManager = PinLockManager(
            context = context,
            pinHasher = pinHasher,
            lockoutPolicy = lockoutPolicy,
            timeProvider = fakeTimeProvider,
            dataStore = testDataStore
        )

        val testDispatcher = UnconfinedTestDispatcher()
        val fakeSettingsDao = io.mockk.mockk<com.uniquemakeover.admin.core.database.dao.SettingsDao>(relaxed = true)
        pinRepository = PinRepositoryImpl(pinLockManager, fakeSettingsDao, testDispatcher)
        verifyPinUseCase = VerifyPinUseCase(pinRepository)
        getLockoutStateUseCase = GetLockoutStateUseCase(pinRepository)

        viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)
    }

    @Test
    fun enteringFiveIncorrectPins_activatesLockoutAndDisablesKeypad() {
        var authenticated = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PinLockRoute(
                    viewModel = viewModel,
                    onAuthenticated = { authenticated = true }
                )
            }
        }

        // Enter wrong PIN 4 times: "0000" x 4
        repeat(4) {
            repeat(4) {
                composeTestRule.onNodeWithText("0").performClick()
            }
            composeTestRule.waitUntil(TIMEOUT_MILLIS) {
                viewModel.uiState.value.isError
            }
            composeTestRule.onNodeWithText("Incorrect PIN. Try again.").assertIsDisplayed()
        }

        // 5th incorrect attempt
        repeat(4) {
            composeTestRule.onNodeWithText("0").performClick()
        }
        composeTestRule.waitUntil(TIMEOUT_MILLIS) {
            viewModel.uiState.value.isLocked
        }

        // Lockout activated!
        composeTestRule.onNodeWithText("Too many failed attempts. Try again in 30s").assertIsDisplayed()
        assertThat(authenticated).isFalse()

        // Additional click while locked is dropped
        composeTestRule.onNodeWithText("1").performClick()
        assertThat(viewModel.uiState.value.digitsEntered).isEqualTo(0)
    }
}
