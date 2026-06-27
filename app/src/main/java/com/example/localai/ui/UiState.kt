package com.example.localai.ui

data class UiState(
    val outputText: String = "Ready.",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isModelLoaded: Boolean = false // Add this
)