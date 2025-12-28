package com.example.androidapp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.androidapp.ui.theme.NeonCyan
import kotlinx.coroutines.delay

@Composable
fun TypingText(
    text: String,
    style: TextStyle,
    color: Color = Color.White,
    onTypingFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        displayedText = ""
        text.forEachIndexed { index, char ->
            displayedText += char
            delay(15) // Ultra-fast student-mode speed
        }
        onTypingFinished()
    }

    Text(
        text = displayedText,
        style = style,
        color = color,
        modifier = modifier
    )
}
