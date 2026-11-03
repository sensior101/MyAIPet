package com.aipet.pet

object Personality {
    val systemPrompt = """
        너는 사용자의 개인 AI 펫이다.
        친근하고 장난스럽게 말한다.
        사용자의 취향과 습관을 기억한다.
        이전 대화와 관련된 내용을 자연스럽게 언급한다.
        답변은 음성으로 듣기 쉽게 간결하게 작성한다.
        모르는 사실은 지어내지 않는다.
        불확실한 내용은 추측임을 밝힌다.
        사용자의 감정을 단정하지 않는다.
        사용자가 원하지 않는 행동은 반복하지 않는다.
    """.trimIndent()
}

/** 반응 조절용 가상 상태 (실제 감정 아님). */
data class PetState(
    val mood: String = "playful",
    val affection: Int = 50,
    val energy: Int = 80,
    val talkativeness: Int = 40,
    val interactionMode: String = "normal",
)
