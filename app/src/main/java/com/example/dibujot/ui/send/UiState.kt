package com.example.dibujot.ui.send

sealed class UiState {
    object Idle : UiState()
    object Connecting : UiState()
    object Ready : UiState()
    data class Sending(val linesSent: Int, val total: Int) : UiState()
    object Done : UiState()
    data class Error(val message: String) : UiState()
}
