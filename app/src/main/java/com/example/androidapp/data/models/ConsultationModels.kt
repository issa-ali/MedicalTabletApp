package com.example.androidapp.data.models

import androidx.compose.ui.graphics.vector.ImageVector

data class Medicine(
    val name: String,
    val price: String,
    val description: String = "",
    val id: String? = null
)

data class VitalSign(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val unit: String,
    val instruction: String,
    var value: String? = null
)

data class ChatMessage(val role: String, val content: String, val id: String = java.util.UUID.randomUUID().toString())
