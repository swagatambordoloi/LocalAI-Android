package com.example.localai.data

enum class AIModel(val path: String, val ramCostMb: Int) {
    // This perfectly matches your Windows file path
    LLAMA_3_2("models/gemma2-2b-it-cpu-int8.task", 1500),

    // We will leave this one alone for now
    MOONDREAM_2("models/moondream2_4b_quantized.tflite", 1800)
}