# AI PET

스마트폰(Android)을 두뇌로 쓰는 로컬 AI 반려인형 프로젝트.
인형은 카메라·마이크·스피커·센서 입출력 장치, 스마트폰은 AI 추론·기억·성격 관리를 담당한다.

## 현재 단계: Phase 1 — 텍스트 기반 AI 펫

- [x] Android 프로젝트 생성 (Kotlin / Jetpack Compose)
- [x] 기본 채팅 UI + 성격 프롬프트 + `PetState`
- [ ] GitHub 레포지토리 생성
- [ ] llama.cpp로 Qwen3-1.7B 로컬 실행 (`LlmEngine` 구현)
- [ ] 대화 기록 관리 / 응답 속도·RAM 측정
- [ ] Qwen3-4B 비교

## 구조

```
app/src/main/java/com/aipet/
├─ ai/       LlmEngine (현재 EchoLlmEngine 임시 구현)
├─ pet/      Personality, PetState
├─ memory/   (Phase 4)
└─ ui/       ChatScreen, ChatViewModel
```

## 실행

Android Studio에서 이 폴더를 열면 Gradle Wrapper가 설정되고 빌드된다. (minSdk 29, JDK 17)
