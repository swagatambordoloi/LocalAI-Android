package com.example.localai.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow // Ensure this is imported
import kotlinx.coroutines.withContext
import java.io.File

class LiteRTRepository(
    private val context: Context,
    private val chatDao: ChatDao // DAO injected here
) {

    private var _activeEngine: Engine? = null
    val activeEngine: Engine? get() = _activeEngine

    suspend fun importModelFromUri(
        uri: Uri,
        fileName: String,
        onProgress: (Int) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val destinationDir = File(context.filesDir, "models")
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val destinationFile = File(destinationDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            loadModelByPath(destinationFile.absolutePath)
        }
    }

    suspend fun loadModelByPath(path: String) {
        withContext(Dispatchers.IO) {
            try {
                _activeEngine?.close()
                val config = EngineConfig(
                    modelPath = path,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                _activeEngine = Engine(config)
                _activeEngine?.initialize()
            } catch (e: Exception) {
                Log.e("MODEL_ERROR", "Engine failed: ${e.message}")
            }
        }
    }

    // Database interaction methods
    // Ensure these methods use the 'chatDao' property defined in the constructor
    fun getChatHistory(): Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun saveMessage(message: ChatMessage) = chatDao.insert(message)

    fun unloadCurrentModel() {
        _activeEngine?.close()
        _activeEngine = null
    }
}