package com.example.localai.domain

import android.app.ActivityManager
import android.content.Context
import com.example.localai.data.AIModel
import com.example.localai.data.LiteRTRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MemoryManager(
    private val context: Context,
    val repository: LiteRTRepository
) {
    private val memoryMutex = Mutex()
    val activeMemoryUsageMb = MutableStateFlow(0)

    // Store the path of the currently loaded model to avoid redundant reloads
    private var loadedModelPath: String? = null

    private val safeAppLimitMb: Int
        get() {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return if (memoryInfo.totalMem > 10_000_000_000L) 6500 else 3500
        }

    suspend fun requestModelMode(targetModel: AIModel, modelPath: String) = memoryMutex.withLock {
        // Prevent reloading the same model path
        if (loadedModelPath == modelPath) return@withLock

        // Eviction logic
        if (activeMemoryUsageMb.value + targetModel.ramCostMb > safeAppLimitMb) {
            repository.unloadCurrentModel()
            activeMemoryUsageMb.update { 0 }
            loadedModelPath = null
        }

        repository.loadModelByPath(modelPath)
        loadedModelPath = modelPath
        activeMemoryUsageMb.update { targetModel.ramCostMb }
    }
}