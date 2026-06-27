package com.example.localai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String, // New: Groups messages into a "chat"
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)