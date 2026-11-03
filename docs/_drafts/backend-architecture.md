# AI PET 백엔드 구조

> 이 프로젝트에는 **서버가 없다.** "백엔드"는 휴대폰 앱 안에서 UI 아래쪽에 있는 모든 것(AI 추론, 기억, 상태, 기기 통신)을 말한다.
> 외부와 통신하는 경우는 선택 기능인 웹 검색 하나뿐이다.

## 1. 전체 구조도

```text
┌──────────────────────────────────────────────────────────────────┐
│ Presentation (Compose UI + ViewModel)                            │
│  Chat · Memory · PetStatus · Device · Settings                   │
└───────────────┬──────────────────────────────────────────────────┘
                │ StateFlow / suspend 호출 (UI는 Orchestrator만 안다)
┌───────────────▼──────────────────────────────────────────────────┐
│ Orchestration                                                     │
│  PetBrain ── 대화 1턴을 처음부터 끝까지 지휘                       │
│  ConversationSession ── 대화 상태 머신(IDLE→LISTENING→...)        │
│  InteractionScheduler ── 능동 대화 / 일정 / 이벤트 트리거          │
│  EventBus ── 모든 입력 이벤트를 하나의 Flow로 합침                 │
└──┬───────────┬────────────┬─────────────┬───────────────┬────────┘
   │           │            │             │               │
┌──▼─────┐ ┌───▼─────┐ ┌────▼──────┐ ┌────▼──────┐ ┌──────▼───────┐
│ AI     │ │ Speech  │ │ Memory    │ │ Pet       │ │ Device       │
│ Llm    │ │ Vad     │ │ Retriever │ │ StateStore│ │ Connection   │
│ Vision │ │ Stt     │ │ Extractor │ │ Persona   │ │ AudioIO      │
│ Prompt │ │ Tts     │ │ Manager   │ │ Policy    │ │ CameraSource │
│ Builder│ │         │ │           │ │           │ │ Controller   │
└──┬─────┘ └───┬─────┘ └────┬──────┘ └────┬──────┘ └──────┬───────┘
   │           │            │             │               │
┌──▼───────────▼─────────┐ ┌▼─────────────▼──┐ ┌──────────▼────────┐
│ Native Runtime (JNI)   │ │ Data (Room)      │ │ Transport         │
│ llama.cpp · sherpa-onnx│ │ DAO · FTS ·      │ │ Phone · BT Audio  │
│ ModelManager           │ │ 파일 저장소       │ │ BLE · Wi-Fi       │
└────────────────────────┘ └──────────────────┘ └───────────────────┘

[항상 실행] PetService (Foreground Service): Orchestration 이하 전체를 소유
            → 화면이 꺼져도 인형과 대화 가능
```

### 의존성 규칙
- 위에서 아래로만 의존한다. 하위 계층은 상위 계층을 모른다.
- 각 엔진은 **인터페이스 + 교체 가능한 구현**으로 만든다. (모델·인형·통신 방식 교체 대비)
- UI는 `PetBrain`과 Repository의 Flow만 본다. 엔진을 직접 호출하지 않는다.
- 무거운 모델은 `ModelManager`가 한 곳에서 불러오고 내린다. (RAM 부족 대응)

## 2. 패키지 구성

```text
com.aipet
├─ app/            PetService, DI(AppContainer)
├─ orchestration/  PetBrain, ConversationSession, InteractionScheduler, EventBus
├─ ai/             LlmEngine, VisionEngine, PromptBuilder, ModelManager
├─ speech/         Vad, SttEngine, TtsEngine
├─ memory/         MemoryRepository, MemoryRetriever, MemoryExtractor, MemoryManager
├─ pet/            PetStateStore, Persona, ProactivePolicy
├─ device/         DeviceConnection, AudioIO, CameraSource, DeviceController
├─ data/           Room DB, DAO, Entity
├─ online/         WebSearch (선택, 기본 OFF)
└─ ui/             화면 + ViewModel
```

DI는 처음엔 직접 만든 `AppContainer`로 충분하다. 규모가 커지면 Hilt로 바꾼다.

## 3. 대화 1턴의 흐름

```text
EventBus: ButtonPressed / WakeWord
  → ConversationSession: IDLE → LISTENING   (LED: LISTENING)
  → AudioIO.mic → Vad → (SPEECH_END)
  → SttEngine.transcribe                    → THINKING
  → MemoryRetriever.retrieve(text)
  → PromptBuilder.build(persona, petState, memories, history, text)
  → LlmEngine.generate (토큰 스트림)
  → SentenceSplitter → TtsEngine.speak       → SPEAKING
  → AudioIO.speaker
  → IDLE
  └─(비동기) MemoryExtractor → MemoryManager.commit
  └─(비동기) PetStateStore.apply(USER_SPOKE)
```

끼어들기: SPEAKING 중 Vad가 SPEECH_START를 내면 → `TtsEngine.stop()` + LLM 생성 취소 → LISTENING.

## 4. 인터페이스 정의 (Kotlin)

### 4.1 Orchestration

```kotlin
interface PetBrain {
    val session: StateFlow<SessionState>
    val transcript: StateFlow<List<ChatMessage>>
    suspend fun handleText(text: String): Flow<String>      // 텍스트 채팅 (Phase 1)
    suspend fun startListening()                             // 버튼/호출어
    fun interrupt()                                          // 끼어들기
}

enum class SessionState { IDLE, LISTENING, THINKING, SPEAKING, LOOKING, ERROR }

interface EventBus {
    val events: SharedFlow<PetEvent>
    suspend fun emit(event: PetEvent)
}

sealed interface PetEvent {
    data object ButtonPressed : PetEvent
    data object WakeWord : PetEvent
    data class Touched(val zone: String, val durationMs: Int) : PetEvent
    data class Motion(val kind: String) : PetEvent           // PICKED_UP, SHAKEN...
    data class ScheduleDue(val memoryId: Long) : PetEvent
    data object LongSilence : PetEvent
    data class Battery(val percent: Int, val charging: Boolean) : PetEvent
    data class Connection(val state: ConnectionState) : PetEvent
}

interface InteractionScheduler {
    fun schedule(memoryId: Long, at: Instant, message: String)
    fun cancel(memoryId: Long)
}
```

### 4.2 AI

```kotlin
interface LlmEngine {
    val isLoaded: Boolean
    suspend fun load(model: ModelSpec)
    fun generate(prompt: Prompt, params: GenParams = GenParams()): Flow<String>
    suspend fun unload()
}

data class GenParams(val maxTokens: Int = 256, val temperature: Float = 0.7f, val stop: List<String> = emptyList())
data class Prompt(val system: String, val messages: List<ChatMessage>)

interface VisionEngine {
    suspend fun describe(image: Bitmap, question: String?): String
    suspend fun embed(image: Bitmap): FloatArray
}

interface PromptBuilder {
    fun build(persona: Persona, state: PetState, memories: List<Memory>,
              history: List<ChatMessage>, input: String, vision: String? = null): Prompt
}

interface ModelManager {                     // RAM 관리: 동시에 올릴 모델 결정
    val loaded: StateFlow<Set<ModelKind>>
    suspend fun ensure(kind: ModelKind)
    suspend fun release(kind: ModelKind)
}
enum class ModelKind { LLM, VLM, STT, TTS, VAD }
data class ModelSpec(val kind: ModelKind, val path: String, val contextSize: Int = 4096)
```

### 4.3 Speech

```kotlin
interface Vad {
    fun process(frame: ShortArray): VadEvent?                // 16kHz mono, 30ms 프레임
    fun reset()
}
enum class VadEvent { SPEECH_START, SPEECH_END }

interface SttEngine {
    suspend fun transcribe(pcm: ShortArray, sampleRate: Int = 16_000): SttResult
}
data class SttResult(val text: String, val confidence: Float, val latencyMs: Long)

interface TtsEngine {
    suspend fun synthesize(text: String, voice: String = "default"): AudioClip
    fun stop()
}
data class AudioClip(val pcm: ShortArray, val sampleRate: Int)
```

### 4.4 Memory

```kotlin
interface MemoryRepository {
    fun observeAll(): Flow<List<Memory>>
    suspend fun search(query: String, limit: Int = 5): List<Memory>
    suspend fun upsert(memory: Memory): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}

interface MemoryRetriever {                  // 검색 전략 (FTS → 추후 벡터 검색)
    suspend fun retrieve(input: String, history: List<ChatMessage>): List<Memory>
}

interface MemoryExtractor {                  // LLM으로 기억 후보 추출
    suspend fun extract(turn: List<ChatMessage>): List<MemoryCandidate>
}

interface MemoryManager {                    // 중복/충돌 판단 후 저장
    suspend fun commit(candidate: MemoryCandidate): MemoryDecision
    suspend fun forget(query: String): Int   // "그 기억 삭제해"
}

interface VisualMemoryRepository {
    suspend fun findSimilar(embedding: FloatArray, threshold: Float = 0.8f): VisualMemory?
    suspend fun save(item: VisualMemory): Long
}
```

### 4.5 Pet

```kotlin
interface PetStateStore {
    val state: StateFlow<PetState>
    suspend fun apply(event: PetEvent)       // 상태 변화 규칙
    suspend fun reset()
}

interface ProactivePolicy {                  // 능동 대화 허용 여부
    fun canSpeakNow(now: Instant, lastSpokenAt: Instant?): Boolean
}
```

### 4.6 Device (인형 / 휴대폰 공통)

```kotlin
interface DeviceConnection {
    val state: StateFlow<ConnectionState>
    val events: Flow<PetEvent>
    suspend fun connect(id: String)
    suspend fun disconnect()
}

interface AudioIO {                          // 구현: PhoneAudioIO / BtHeadsetAudioIO / DollWifiAudioIO
    fun micFrames(): Flow<ShortArray>
    suspend fun play(clip: AudioClip)
    fun stopPlayback()
}

interface CameraSource {                     // 구현: PhoneCameraSource / DollCameraSource
    suspend fun capture(): Bitmap
    fun setEnabled(enabled: Boolean)
}

interface DeviceController {
    suspend fun setLed(pattern: LedPattern)
    suspend fun setVolume(level: Int)
    suspend fun sleep(deep: Boolean)
}
enum class LedPattern { OFF, IDLE, LISTENING, THINKING, SPEAKING, CAMERA_ON, LOW_BATTERY }
```

### 4.7 Online (선택)

```kotlin
interface WebSearch {
    suspend fun needsSearch(input: String): Boolean
    suspend fun search(query: String, limit: Int = 3): List<SearchResult>
}
data class SearchResult(val title: String, val snippet: String, val url: String)
```

## 5. 단계별 구현체

| 인터페이스 | Phase 1 | Phase 3~5 | Phase 6+ |
|---|---|---|---|
| LlmEngine | EchoLlmEngine → LlamaCppEngine | 동일 | 동일 |
| AudioIO | 없음 | PhoneAudioIO, BtHeadsetAudioIO | DollWifiAudioIO |
| CameraSource | 없음 | PhoneCameraSource | DollCameraSource |
| DeviceConnection | 없음 | 없음 | BleDeviceConnection |
| MemoryRetriever | 없음 | FtsMemoryRetriever | + 벡터 검색 |

인형이 바뀌어도 `AudioIO` / `CameraSource` / `DeviceConnection` 구현체만 바꾸면 된다. 기억·성격·상태는 Room DB에 그대로 남는다.

## 6. 스레드 / 수명 주기

- `PetService`(Foreground Service)가 `CoroutineScope(SupervisorJob() + Dispatchers.Default)`를 소유한다.
- LLM, STT, TTS 추론은 각각 전용 단일 스레드 디스패처에서 실행한다. (네이티브 런타임은 스레드 안전하지 않음)
- DB 작업은 `Dispatchers.IO`에서 실행한다.
- 오디오 캡처는 별도 스레드에서 실행하고 `Flow`로 내보낸다.
- 화면이 꺼져도 Service가 살아 있는지 Phase 8에서 기종별로 검증한다.
