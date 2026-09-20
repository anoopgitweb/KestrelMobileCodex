package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kestrel Intelligence Preview", appName)
  }

  @Test
  fun `four digit pin is salted and verified locally`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = com.example.data.local.AuthPreferences(context)
    preferences.clearSession()
    preferences.setPin("4826")

    assertEquals(true, preferences.verifyPin("4826"))
    assertEquals(false, preferences.verifyPin("4825"))
    assertEquals(false, preferences.verifyPin("48261"))
  }
}
