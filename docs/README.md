# AI PET 설계 문서

| # | 영역 | 폴더 | 상태 |
|---|---|---|---|
| 1 | 대표·세부 시나리오, 행위 다이어그램 | [01-scenarios](01-scenarios/) | 초안 완료 |
| 2 | 시스템 구조, 인터페이스 | [02-architecture](02-architecture/) | 초안 완료 (MCP 해석 확인 필요) |
| 3 | 요구사항, 기획·정책 | [03-requirements](03-requirements/) | 초안 완료 (수치는 실측 후 조정) |

```text
docs/
├─ README.md                    ← 이 파일 (목차)
├─ 01-scenarios/
│  ├─ README.md                 작성 가이드
│  ├─ golden-scenario.md        대표(골든 샘플) 시나리오
│  ├─ detailed-scenarios.md     정상 / 예외 / 불명확 요청 세부 시나리오
│  └─ diagrams/                 시퀀스·플로우·상태 머신 (Mermaid 원본, 이미지)
├─ 02-architecture/
│  ├─ README.md
│  ├─ layers.md                 레이어별 역할·책임
│  ├─ interfaces.md             레이어 간 / 레이어 내부 인터페이스
│  ├─ api-examples/             요청·응답 JSON 예시
│  └─ diagrams/                 컴포넌트·클래스 다이어그램
├─ 03-requirements/
│  ├─ README.md
│  ├─ functional.md             기능 요구사항 (MoSCoW + 인수 조건)
│  ├─ non-functional.md         비기능 요구사항
│  ├─ scope.md                  구현 범위 / 제외 범위
│  ├─ policies.md               확인·재질문·실패 안내·fallback 정책
│  └─ tech-policies.md          로컬/클라우드, 컨텍스트 저장, 목표 응답시간
└─ _drafts/                     이전 초안 (backend-architecture.md, interfaces.json 옮길 곳)
```

## 작성 규칙
- 다이어그램은 Mermaid 코드블록으로 문서 안에 작성하고, 필요하면 `diagrams/`에 원본(`.mmd`)과 이미지를 둔다.
- ID 규칙: 시나리오 `SC-xx`, 기능 요구사항 `FR-xx`, 비기능 `NFR-xx`, 정책 `POL-xx`, 인터페이스 `IF-xx`.
- 문서끼리는 ID로 연결한다. (예: FR-03 ↔ SC-02 ↔ IF-05)
