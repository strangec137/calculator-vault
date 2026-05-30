package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.VaultViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calculator", appName)
  }

  @Test
  fun `test default PIN unlocking`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VaultViewModel(app)

    var navigated = false
    val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
      viewModel.navigateToVault.collect {
        navigated = true
      }
    }

    // Default PIN is "0.000"
    println("Initial: ${viewModel.displayText.value}, PIN: ${viewModel.secretPin.value}")
    viewModel.onCalculatorKeyPress("0")
    println("After 0: ${viewModel.displayText.value}")
    viewModel.onCalculatorKeyPress(".")
    println("After .: ${viewModel.displayText.value}")
    viewModel.onCalculatorKeyPress("0")
    println("After second 0: ${viewModel.displayText.value}")
    viewModel.onCalculatorKeyPress("0")
    println("After third 0: ${viewModel.displayText.value}")
    viewModel.onCalculatorKeyPress("0")
    println("After fourth 0: ${viewModel.displayText.value}")

    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(navigated)
    job.cancel()
  }

  @Test
  fun `test direct unlock backdoor`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VaultViewModel(app)

    var navigated = false
    val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
      viewModel.navigateToVault.collect {
        navigated = true
      }
    }

    viewModel.unlockDirectly()

    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(navigated)
    job.cancel()
  }
}
