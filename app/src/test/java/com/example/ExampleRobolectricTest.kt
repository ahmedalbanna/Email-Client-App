package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
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
    // Phase 4: app_name has en fallback "Email" and ar "البريد الإلكتروني"
    assertTrue(appName == "البريد الإلكتروني" || appName == "Email")
  }

  @Test
  @Config(qualifiers = "ar", sdk = [36])
  fun `read Arabic string`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertTrue(appName == "البريد الإلكتروني")
  }
}
