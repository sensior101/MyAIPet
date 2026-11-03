package com.aipet.ai

/** 로컬 LLM 추상화. Phase 1에서 llama.cpp(Qwen3-1.7B) 구현으로 교체한다. */
interface LlmEngine {
    suspend fun generate(systemPrompt: String, history: List<ChatMessage>): String
}

data class ChatMessage(val role: Role, val text: String) {
    enum class Role { USER, PET }
}

/** 모델 연동 전 UI 확인용 임시 엔진. */
class EchoLlmEngine : LlmEngine {
    override suspend fun generate(systemPrompt: String, history: List<ChatMessage>): String =
        "(모델 미연결) 방금 \"${history.lastOrNull()?.text.orEmpty()}\"라고 했지?"
}
