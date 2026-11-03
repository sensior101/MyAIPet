package com.aipet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipet.ai.ChatMessage
import com.aipet.ai.EchoLlmEngine
import com.aipet.ai.LlmEngine
import com.aipet.pet.Personality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(private val engine: LlmEngine = EchoLlmEngine()) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun send(text: String) {
        if (text.isBlank() || _busy.value) return
        _messages.update { it + ChatMessage(ChatMessage.Role.USER, text.trim()) }
        viewModelScope.launch {
            _busy.value = true
            val reply = engine.generate(Personality.systemPrompt, _messages.value)
            _messages.update { it + ChatMessage(ChatMessage.Role.PET, reply) }
            _busy.value = false
        }
    }
}
