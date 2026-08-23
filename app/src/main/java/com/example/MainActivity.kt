package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EmailApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EmailViewModel
import com.example.viewmodel.EmailViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = application as EmailApplication
    val factory = EmailViewModelFactory(app.container.emailRepository, application)
    setContent {
      val dynamicColor by app.container.themePrefs.dynamicColorFlow.collectAsState(initial = false)
      val forceDark by app.container.themePrefs.forceDarkFlow.collectAsState(initial = null)
      val darkTheme = forceDark ?: isSystemInDarkTheme()
      MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        val viewModel: EmailViewModel = viewModel(factory = factory)
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          EmailApp(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
