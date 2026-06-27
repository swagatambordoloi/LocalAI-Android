package com.example.localai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localai.data.ChatMessage // 1. ADD THIS IMPORT
import com.example.localai.data.LiteRTRepository
import com.example.localai.data.AIModel
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: LiteRTRepository,
    application: Application
) : AndroidViewModel(application) {
    val activeSessionId = MutableStateFlow(UUID.randomUUID().toString())

    // Switch the view to a specific session
    fun loadSession(sessionId: String) {
        activeSessionId.value = sessionId
    }
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    val chatHistory: Flow<List<ChatMessage>> = repository.getChatHistory()

    // Ensure this is properly initialized
    private var currentConversation: com.google.ai.edge.litertlm.Conversation? = null

    fun executePrompt(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Save User Message
            repository.saveMessage(ChatMessage(sessionId = activeSessionId.value, role = "user", text = prompt))

            try {
                val engine = repository.activeEngine
                if (engine != null) {
                    if (currentConversation == null) {
                        currentConversation = engine.createConversation()
                    }

                    // 2. Perform Inference using the instance variable
                    val response = currentConversation!!.sendMessage(prompt)
                    val responseText = extractText(response)

                    // 3. Save Model Message
                    repository.saveMessage(ChatMessage(sessionId = activeSessionId.value, role = "model", text = responseText))

                    _uiState.value = _uiState.value.copy(isLoading = false, outputText = responseText)
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, outputText = "Error occurred.")
            }
        }
    }

    // This must be inside the class, not nested elsewhere
    private fun extractText(responseMessage: Any): String {
        return try {
            val parts = responseMessage.javaClass.getMethod("getParts").invoke(responseMessage) as List<*>
            parts.first().toString()
        } catch (_: Exception) {
            responseMessage.toString()
        }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, progress = 0)
            try {
                repository.importModelFromUri(uri, "model.litertlm") { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
                currentConversation = null
                _uiState.value = _uiState.value.copy(isLoading = false, outputText = "Model Ready!")
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, outputText = "Import Failed.")
            }
        }
    }

    fun switchMode(model: AIModel, customPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.loadModelByPath(model.path)
            currentConversation = null
            _uiState.value = _uiState.value.copy(isLoading = false, outputText = "${model.name} loaded")
        }
    }
}
