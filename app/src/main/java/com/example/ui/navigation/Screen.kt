package com.example.ui.navigation

sealed class Screen {
    object Inbox : Screen()
    object Compose : Screen()
    object AddAccount : Screen()
}
