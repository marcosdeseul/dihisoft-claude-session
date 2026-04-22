---
name: plan-issue
description: GitHub 이슈 한 건을 받아 저장소 상태·CLAUDE.md 제약에 맞춰 **분석·분할·계획 수립·하위 이슈 생성**까지 자동으로 끌고 간다. "이슈 분석해줘", "이슈 N번 계획 세워줘", "이거 분할하자" 류 요청에서 호출한다. **구현·커밋·PR 은 범위 밖**(`raise-pr` 또는 자유 구현 흐름). 이 스킬은 이슈 본문·README·기존 코드를 읽어 Context 를 정리하고, 10파일/이슈 한도 등 고유 제약에 걸리면 `x.1/x.2` 형태로 하위 이슈를 쪼갠 뒤 플랜 파일을 남긴다.
---

# plan-issue — 이슈 분석·분할·계획 수립

이 프로젝트(Spring Boot + Gradle + JaCoCo 95% 게이트 + **이슈당 10파일** 규칙)의 고유 제약에 맞게, **GitHub 이슈 한 건**을 받아 실제 착수 가능한 계획 문서와 GitHub 체계를 만든다.

## 전제
- 이슈 번호를 받았고(예: `#8`), 저장소는 clone 된 상태.
- `gh` CLI 인증 완료.
- Plan mode 로 들어온 상태(플랜 파일 경로가 시스템 메시지로 주어짐) 또는 그에 준하는 맥락.
- CLAUDE.md / README.md 가 저장소에 있음 — 이 스킬은 두 문서를 반드시 읽는다.

아직 이슈 번호가 없다면 먼저 사용자와 이슈부터 합의한다(이 스킬 범위 밖).

## 언제 사용하는가
- "이슈 #N 해결 계획 세워줘", "어떻게 쪼개지?" 류 요청
- 범위가 커 보여 10파일 한도 초과가 의심될 때
- 이슈 본문이 모호해 착수 전 범위/의존성 확정이 필요할 때

사용하지 말 것:
- 이미 구현이 끝나 PR 올릴 차례 → `raise-pr`
- 한 줄 오타 수정 같은 자명한 작업 → 바로 구현

## Phase 1 — 이슈 맥락 수집

### 1.1 이슈 본문 + 의존성
```bash
gh issue view <num> --json body,number,title,labels,url,state
```
**반드시 확인**
- 범위 / 결과물 목록 (`JwtTokenProvider`, `POST /api/...` 등 구체 명사)
- `Depends on #X`, `Parent: #Y` 관계
- `Closes #Z` 언급
- 라벨 (부모 라벨은 하위 이슈에 **그대로 승계**)

### 1.2 CLAUDE.md + README.md 정독
- CLAUDE.md: TDD 사이클, 커밋 컨벤션, 10파일/300줄 한도, Edge case 체크리스트
- README.md: **API 스펙이 자주 박혀있다.** 이번 프로젝트에선 `expiresIn` 단위(초), JWT 알고리즘(HS256) 같은 결정적 정보가 README에 있음. 본문 파싱과 코드가 어긋나면 README 쪽이 우선권.

### 1.3 Explore 서브에이전트로 병렬 탐색
스코프가 넓으면 **최대 3개 병렬**. 통상 1개로 충분.

**Explore 에게 반드시 시킬 것**
- 관련 패키지/클래스 트리 (절대 경로)
- 재사용 가능한 기존 자산 (Repository 메서드, DTO, EntityManager, 테스트 유틸)
- 빌드 의존성 (해당 기능에 필요한 lib 가 이미 있나)
- 기존 테스트 패턴 (MockMvc? @Nested? @Tag("playwright")?)
- git 로그 — 의존 이슈가 실제로 머지됐는지(`git log main --oneline | head -30`)
- CI 설정 (`.github/workflows/*.yml`) — jacoco 커버리지 문턱 등

Explore 요청문 템플릿:
```
Explore /<repo-root>.
Issue #<N> requires <short goal>. Report:
1. Existing auth/<area> infrastructure (package tree, key classes with absolute paths)
2. Reusable assets (repository methods, DTO patterns, config beans)
3. Build dependencies relevant to <lib>
4. Testing patterns (framework, @Nested style, Playwright tag usage)
5. Whether #<dep> work is on main (git log check)
6. Relevant conventions from CLAUDE.md (file-count rule, test naming, commit style)
```

## Phase 2 — 제약 체크 및 분할 결정

### 2.1 예상 파일 수 추정
받은 범위로 **수정/신규 파일을 모두 카운트**한다.
- 프로덕션 코드, 테스트 코드, 설정 파일 모두 합산
- Gradle wrapper 자동생성은 제외

**규칙**
- **≤ 10파일** → 단일 이슈/PR 로 진행
- **> 10파일** → 하위 이슈로 분할 권고

### 2.2 분할 단위 — "TDD 한 사이클이 독립적으로 성립하는 feature slice"
흔한 분할 축:
- **인프라 / API / UI** — 예: JWT 인프라+API(백엔드) vs 로그인 UI(프론트)
- **엔티티 / 조회 / 변경** — 예: Post 엔티티+Repository vs 목록 조회 vs 작성/수정/삭제
- **핵심 / 부가** — 핵심 먼저, 부가(페이지네이션·정렬 등) 나중

나쁜 분할 축: "테스트만", "리팩터만" — 독립 feature slice 가 아님.

### 2.3 번호·이름 체계
- 하위 이슈 번호: 부모 `x.y` → `x.y.1`, `x.y.2` (예: 2.2 → 2.2.1, 2.2.2)
- 하위 이슈 제목: **담당자 이니셜 prefix + 번호 + 주제** (예: `kms-2.2.1 JWT 인프라 + 로그인 API`)
  - prefix는 사용자에게 한 번 물어 고정 (AskUserQuestion)
- 브랜치명: `<prefix>/<번호>-<kebab-주제>` (예: `kms/2.2.1-jwt-login-api`)

## Phase 3 — 사용자 확인 (AskUserQuestion)

탐색·제약 체크에서 확정 못 한 것만 **최소 질문으로 확인**. 흔한 질문 축:

1. **분할 여부** — 파일이 10개 근처면: "단일 PR vs 2개 PR?" (추천은 근거와 함께 제시)
2. **담당자 prefix / 이니셜** — 이미 이번 세션에서 `kms` 로 고정됐다면 생략, 처음이면 확인
3. **브랜치명 / 하위 이슈 제목 형식** — 부모 이슈 제목 형식 보여주고 일관성 있게 선택지 제공
4. **기술 선택** — 예: SPA용 localStorage vs httpOnly 쿠키, 동기/비동기, 롤백 전략

묻지 말 것:
- 코드에서 읽으면 되는 것 (패키지 구조, 현재 SecurityConfig 내용 등)
- 이미 이슈 본문에 박혀있는 것

## Phase 4 — Plan 에이전트 (선택)

범위가 크거나 설계 판단이 필요하면 Plan 서브에이전트 1개 사용.
트리비얼한 단일 파일 작업이면 생략.

Plan 에이전트에 제공:
- Phase 1 요약 (파일 경로 + 클래스명 포함)
- 제약 (10파일·300줄·95% 커버리지·TDD 필수)
- 사용자가 고른 옵션 (Phase 3)
- 요청: "step-by-step 구현 계획 + 수정 파일 목록 + TDD 커밋 시퀀스 + 검증 방법"

## Phase 5 — 플랜 파일 작성

시스템 메시지가 지정한 경로(예: `~/.claude/plans/<adjective>.md`)에 **한 파일**로 쓴다. 구조:

```markdown
# Issue #<N> — <제목> 구현 계획

## Context
대상 이슈: <URL>
의존성: #<dep> (상태: 머지됨/진행중)
왜: <해결하려는 문제, 기대 결과>
프로젝트 제약: 10파일·300줄·95% 커버리지·TDD

## 이슈 분할 전략 (필요 시)
| 하위 이슈 제목 | 범위 | 브랜치 |
|---|---|---|
| <prefix>-<x.y.1> ... | ... | <prefix>/<x.y.1>-... |
| <prefix>-<x.y.2> ... | ... | <prefix>/<x.y.2>-... |

## 하위 이슈 <x.y.1> — <제목>
### 추가/수정 파일 (총 N개)
<신규 / 수정 구분, 각 파일 한 줄 요약, 절대 경로>
**파일 합계**: N ✅

### TDD 커밋 시퀀스
1. `test: ... RED`
2. `feat: ...`
3. `feat: ...`
4. `refactor: ...` (선택)

### 검증
- `./gradlew clean build` (커버리지 포함)
- 수동: curl / bootRun + manual-test

## 재사용 자산
- `UserRepository.findByUsername(...)` — <파일 경로>
- ...

## 위험 / 주의
- secret 플레이스홀더, CORS, 보호 엔드포인트 부재 등

## 크리티컬 파일 경로 요약

## 선행 액션 (승인 직후)
1. GitHub 하위 이슈 생성 (부모 본문에 링크 추가, 라벨 승계)
2. `git checkout -b <branch>` 후 RED 테스트부터 착수
```

**원칙**
- 추천안 하나만 기술. 대안 나열 금지.
- 재사용 자산은 절대 경로로 명시 — 구현 시 바로 `import` 가능하도록.
- TDD 커밋 시퀀스는 **CLAUDE.md 규칙의 커밋 메시지 컨벤션**(`test:`, `feat:`, `refactor:`)을 그대로 사용.

## Phase 6 — GitHub 반영 (승인 후)

사용자가 ExitPlanMode 로 플랜을 승인한 뒤에만 수행. **plan mode 중엔 쓰기 금지**.

### 6.1 하위 이슈 생성 (분할했을 경우)
```bash
gh issue create \
  --title "<prefix>-<x.y.1> <주제>" \
  --body "$(cat <<'EOF'
## 목적
...
## 범위
...
## TDD 커밋 시퀀스
...
## 의존성
- Depends on #<dep>
- Parent: #<N>
## 브랜치
`<prefix>/<x.y.1>-...`
EOF
)"
```

생성 즉시 부모 이슈의 라벨을 **그대로 복사**:
```bash
gh issue edit <new-num> --add-label "<label1>,<label2>,<label3>"
```

### 6.2 부모 이슈 본문 갱신
부모 본문 상단에 하위 이슈 체크리스트 섹션 추가:
```markdown
## 하위 이슈 (<prefix> 담당자 분할)
- [ ] #<x.y.1> — <prefix>-<x.y.1> <주제>
- [ ] #<x.y.2> — <prefix>-<x.y.2> <주제>

> CLAUDE.md 의 10파일/이슈 제약을 지키기 위해 2개로 분할.
```
`gh issue edit <parent> --body-file /tmp/parent-body.md`.

### 6.3 브랜치 준비 안내
플랜 파일에 "선행 액션" 으로 이미 적어둠. 구현은 이 스킬 범위 밖 — 이어서 자유 구현 흐름으로 넘어가거나 사용자가 다음 턴에서 지시.

## Phase 7 — ExitPlanMode

`ExitPlanMode` 호출로 계획 승인 대기. `AskUserQuestion` 으로 "플랜 괜찮아요?" 묻기 금지 — 승인은 ExitPlanMode 전용.

---

## 가이드라인 (박스 요약)

- **3단 우선순위**: 이슈 본문 → README → 코드. 충돌 시 README 가 스펙.
- **10파일 엄격**: 경계선(9~11)이면 분할 제안. 애매하면 `git diff --name-only` 로 실제 확인.
- **라벨 승계**: 부모 이슈의 라벨을 하위 이슈에 100% 복사. 누락하면 칸반에서 사라짐.
- **하위 이슈 번호 체계**: `x.y.N` 유지, 담당자 prefix 포함. 독자적 번호 만들지 말 것.
- **브랜치명과 이슈 번호 1:1**: `<prefix>/<번호>-<주제>` 포맷.
- **플랜 파일 = 단일 추천안**: 대안·비교표는 Phase 3 질문에서만. 파일에는 확정안만.
- **Plan mode 쓰기 제약**: 시스템이 지정한 플랜 파일만 편집 가능. GitHub 쓰기(`gh issue create/edit`)는 **승인 후**.
- **금지**: 구현·커밋·PR·푸시 (→ `raise-pr` 스킬 범위).

## 다른 스킬과의 관계

- **후행**: `raise-pr` — 이 스킬이 만든 플랜·브랜치·하위 이슈를 받아 PR 라이프사이클 진행.
- **호출 가능**: `manual-test` — Phase 1에서 기존 UI 동작을 확인하고 싶을 때(드묾).
- **범위 밖**: `review` / `security-review` — 본 스킬은 코드 리뷰를 하지 않음.

---

## 실행 예시 — 이번 세션 #8 흐름 (JWT + 로그인 API + UI)

```
Phase 1: gh issue view 8 → 범위 파악(JwtTokenProvider, POST /api/auth/login, 로그인 UI)
         Explore 1개 에이전트로 코드베이스 탐색 → User/AuthService/SignupController 존재, JJWT 0.12.6 의존성 있음,
         CLAUDE.md 10파일 제약, application.yml 에 app.jwt.secret 플레이스홀더, README 에 HS256/expiresIn=3600초 스펙
Phase 2: 예상 파일 12+ → 분할 필요
Phase 3: AskUserQuestion — 분할여부/UI 토큰 처리/브랜치명 → 2개 분할 + kms/2.2.1-... / JS+localStorage 선택
         AskUserQuestion 2회차 — 하위 이슈 제목 형식 → "kms-x.y.N 주제"
Phase 5: plans/linked-pondering-rain.md 작성
Phase 6: gh issue create ×2 (#13, #14) → 라벨 승계(type:feature, area:auth, priority:high)
         gh issue edit 8 --body ... (하위 이슈 체크리스트 추가)
Phase 7: ExitPlanMode → 승인 → 이후 kms/2.2.1 브랜치 착수는 자유 구현 흐름으로
```
