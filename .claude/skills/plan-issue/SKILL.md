---
name: plan-issue
description: GitHub 이슈 번호를 받아 본문·레이블·의존 이슈를 `gh`로 수집하고, 코드베이스를 탐색해 CLAUDE.md 규칙(10파일/이슈, 300줄/파일, TDD 필수)을 반영한 구현 플랜을 `.claude/plans/<N>-<slug>.md` 에 저장한다. 파일 수 제약으로 분할이 필요하면 하위 이슈 발행·브랜치 생성까지 **사용자 승인 후** 준비한다. 사용자가 "#N 이슈 계획", "plan-issue N", "이슈 N번 분석해줘", "N번 작업하자" 등으로 요청할 때 호출한다. 실제 코드 구현(RED/GREEN 커밋)과 PR 발행은 범위 밖(`raise-pr` 또는 자유 구현 흐름).
---

# plan-issue — GitHub 이슈를 프로젝트 규칙에 맞춰 실행 가능한 플랜으로

이 프로젝트(Spring Boot + Gradle + JaCoCo 95% + TDD 필수 + 10파일/이슈 한도) 고유 규칙에 맞춰, **이슈 번호 하나를 받으면 구현 착수 직전까지의 준비**(플랜 작성 → 필요 시 분할 → 브랜치 생성)를 일관 처리한다.

## 전제

- `gh` CLI 인증 완료 (`gh auth status` 가 `Logged in to github.com` 를 반환)
- 원격 `origin` 이 GitHub 저장소로 설정됨
- 프로젝트 루트에 `CLAUDE.md` 존재 (규칙 근거)
- 이슈 번호를 사용자가 명시적으로 전달

gh 미인증 시: **이슈 조회 단계에서 중단**하고 사용자에게 `gh auth login` 요청. `WebFetch` 폴백은 공개 이슈에 한해 최소한으로만 사용(본문 전체 확보가 어려울 수 있음).

## 언제 사용하는가

- 새 이슈를 받아 어떻게 착수할지 설계해야 할 때
- 기존 이슈가 커서 하위 분할이 필요한지 판단해야 할 때
- 담당자 구분을 위해 개인 prefix(예: `[JY]`) 를 붙여 병렬 작업을 열어야 할 때
- TDD Phase 지도와 파일 카운트 예측이 필요한 모든 작업

사용하지 말 것:
- 이미 구현이 끝났고 PR만 올리면 될 때 → `raise-pr`
- 트리비얼한 1-파일 수정 → 바로 구현 흐름으로
- UI 육안 시연 → `manual-test`

## 입력 (사용자가 전달)

- **필수**: 이슈 번호 1개 이상 (예: `#8`, `plan-issue 8`)
- **선택**: 작업자 prefix (예: `[JY]` — 동일 이슈를 여러 담당자가 병렬 작업할 때 구분)
- **선택**: base 브랜치 (기본 `origin/main`)

prefix 필요 여부가 불분명하면 Phase 3에서 `AskUserQuestion` 으로 확인.

## Phase 1 — 이슈 수집

### 1.1 메인 이슈 본문 조회
```bash
gh issue view <N> --repo <owner>/<repo> \
  --json number,title,body,labels,assignees,state
```

- 본문에서 다음 패턴 추출:
  - `Parent: #X`, `부모: #X`
  - `Depends on #X`, `선행: #X`, `의존: #X`
  - `Related #X`, `참고 #X`
  - 하위 체크박스 `- [ ] #Y ...`

### 1.2 관련 이슈 조회
추출된 이슈 번호 각각 `gh issue view` 로 title + body 로드(전문 로딩은 생략). 특히:
- 부모가 에픽이면 에픽 본문에서 형제 이슈 파악 (이미 분할된 범위가 있는지)
- 같은 에픽 하위에 **다른 담당자 이슈가 있다면 prefix 분할이 관례화된 것**(이 프로젝트의 `kms-*`, `[JY]` 패턴)

### 1.3 gh 부재 시
- `which gh` 로 확인 후 없으면: "브랜치·이슈 준비가 필요한 스킬이라 `gh` 인증이 전제입니다. `brew install gh && gh auth login` 후 다시 호출하세요." 안내하고 **중단**
- 임시 폴백: `WebFetch https://github.com/<owner>/<repo>/issues/<N>` 로 공개 이슈 본문 요약만 받아 Phase 2로 진행 (Phase 5는 불가 → 사용자에게 명시)

## Phase 2 — 코드베이스 탐색

### 2.1 CLAUDE.md 로드
`Read /CLAUDE.md` — 아래 규칙 추출하여 Phase 3 판단 근거로 사용:
- 파일 수 제약 (이 프로젝트: 10 / 이슈)
- 파일당 라인 제약 (이 프로젝트: <300)
- TDD 요구 (RED→GREEN→REFACTOR 사이클, 커밋 컨벤션 `test:` `feat:` `refactor:`)
- 테스트 구조 기준 (통합 우선, 모킹 최소화, 한글 네이밍)
- 예외 규정 (실험용 스크래치, 설정/빌드 파일, 단순 DTO)

### 2.2 Explore 병렬 탐색
`Explore` 서브에이전트 1~3개 동시 기동:

| Focus | 질문 예시 |
|---|---|
| 재사용 가능한 기존 구성 | "이슈 범위에 해당하는 기능의 유사 구현이 이미 있는가? 어느 파일?" |
| 패턴·스타일 | "기존 테스트·컨트롤러·서비스의 네이밍·구조 관례는?" |
| 의존 배선 | "새 기능이 끼어들 Security/Config/DI 지점은?" |

에이전트 프롬프트에 반드시 포함: "작성된 파일 덤프 대신 경로 + 3~5줄 인용만. 과도한 출력 금지. 한국어 답변."

### 2.3 재사용 컴포넌트 테이블 구성
| 구성 | 경로 | 역할 |
|---|---|---|
| 예: `GlobalExceptionHandler` | `common/GlobalExceptionHandler.java` | `IllegalArgumentException → 400` 자동 변환 — 그대로 활용 |

Phase 4 플랜 파일에 그대로 수록.

## Phase 3 — 범위 검증 + 분할 결정

### 3.1 예상 파일 수 산정
Phase 2 탐색에 기반해 **이슈 한 건이 만들/고칠 파일 목록**을 표로 정리.
프로덕션(새 파일 + 기존 수정) + 테스트(새 파일 + 기존 수정) 합산.

- Gradle wrapper 자동 생성물·`application.yml` 값 조정만은 **집계 제외** (예외 규정)
- 애매한 건 포함 — 초과 경계에서 안전 마진

### 3.2 10파일 초과 처리
초과하거나 ≥9로 빠듯하면 `AskUserQuestion` 으로 다음 중 선택:

1. **타이트 통합** — 일부 테스트(예: Playwright, 필터 단위) 생략해 10개로 압축. Followup 이슈 메모 보존
2. **하위 분할** — 예: `2.2.1 REST+API`, `2.2.2 UI+E2E` 두 개로 나눠 신규 이슈 발행 (Phase 5에서 실제 발행)
3. **한도 초과 허용** — 사용자 사전 합의(foundation류 예외)가 있을 때만

선택에 따라 Phase 4 플랜의 구조가 달라진다.

### 3.3 Prefix 확인
에픽 아래에 이미 타 담당자의 분할 이슈가 있거나 사용자가 병렬 담당 의사를 보이면 `AskUserQuestion`:
- "신규 이슈·브랜치에 본인 이니셜/prefix를 붙이시겠어요? (예: `[JY]`, `jy/...`)"

## Phase 4 — 플랜 파일 작성

### 4.1 저장 경로
- 기본: `.claude/plans/<이슈번호>-<제목-slug>.md` (프로젝트 루트 기준)
- slug는 한글 제거 + 영문 kebab-case ≤ 40자
- 예: `.claude/plans/17-jy-jwt-login-api.md`

사용자가 `~/.claude/plans/` 를 선호하면 override.

### 4.2 템플릿 (섹션 순서 고정)

```markdown
# Issue #<N> — <요약>

## Context
<왜 이 작업을 하는가. 무엇을 해결하는가. 직전 단계 현황.>

## 분할 전략 (분할 시에만)
| 하위 | 제목 | 브랜치 | 예상 파일 수 |
...

## 사전 단계 (Plan 승인 직후)
1. 브랜치 생성 명령
2. (필요 시) 하위 이슈 발행
3. 원본 이슈 본문 갱신 (하위 링크)

## TDD Phase 맵
### Phase 1 — RED: <대상>
- 테스트 파일 경로
- 케이스 목록 (happy + edge: null/빈/공백/경계/중복/권한/만료/형식)
- GREEN 예정 구현 파일·API 시그니처

### Phase 2 — RED: <대상>
...

(REFACTOR 는 필요 시에만 별도 Phase)

## 파일 카운트
프로덕션 X + 테스트 Y = **Z** (CLAUDE.md 한도: 10)
파일 목록 전체:
- 신규 `...`
- 수정 `...`

## 수용 기준
- [ ] 모든 RED 테스트가 먼저 실패하는 커밋 존재
- [ ] `./gradlew test` 통과 + JaCoCo 95% 유지
- [ ] 수동 재현 가능 (curl/브라우저 시나리오)
- [ ] CLAUDE.md 커밋 컨벤션 준수

## 재사용할 기존 구성 요소
(Phase 2.3 표 그대로)

## 검증 (Verification)
1. 단위/통합: `./gradlew test`
2. (선택) Playwright: `./gradlew test -PincludePlaywright`
3. 수동 재현 스크립트 (curl/브라우저 단계)
4. 회귀 확인 항목

## 커밋 컨벤션
예시 커밋 시퀀스:
test: <대상> RED — <케이스 요약>
feat: <대상> 구현 — 테스트 통과
(선택) refactor: <영역> — <개선>
```

### 4.3 Write
`Write <플랜 경로> <본문>` — 한 번에 작성. 불필요한 장황함 금지, 실행 가능성 우선.

## Phase 5 — 브랜치/이슈 준비 (명시적 승인 게이트)

**이 Phase는 공유 상태를 바꾼다. 반드시 사용자 승인을 얻어야 한다.**

### 5.1 승인 요청
`ExitPlanMode` 로 플랜 승인을 받거나, `AskUserQuestion` 으로 실행 여부 확인:
- "분할 이슈 발행 + 브랜치 생성까지 지금 진행할까요?" (Yes / 플랜만 저장하고 종료)

### 5.2 승인 후 실행 순서
분할이 필요한 경우:
```bash
gh issue create --repo <owner>/<repo> \
  --title "[<prefix>] <x.y.z> <요약>" \
  --body-file /tmp/plan-issue-<xyz>-body.md \
  --label "type:feature" --label "area:<...>" --label "priority:<...>"
```
- Body 는 플랜에서 추출한 "목적 · 범위 · TDD 커밋 시퀀스 · 엣지 케이스 · 의존성 · 브랜치명 · 파일 예상" 섹션
- 반환된 URL·번호를 기록
- 부모 이슈(`gh issue edit <parent>`)에 하위 링크 추가 + 이슈 간 상호 `gh issue comment` 로 Depends 주석

브랜치 생성:
```bash
git fetch origin
git checkout -b issue/<N>-<prefix>-<slug> origin/main
```
- 기존 커밋 없음을 보장(필요 시 `git status` 선제 확인)
- prefix 없으면 `issue/<N>-<slug>`

### 5.3 미승인 또는 스킵
사용자가 "플랜만" 이라고 답하면: 5.1 이후 종료 상태를 명시하고 "이후 착수하려면 이 플랜 경로를 참조하세요" 로 마무리.

## Phase 6 — 인계

### 6.1 다음 단계 안내 (텍스트로만, 자동 실행 금지)
플랜에 기록된 Phase 1 RED 부터 착수하도록 안내:
- TDD 사이클: `test: ... RED` → `feat: ... GREEN` → 필요 시 `refactor:`
- 구현이 끝난 뒤 `raise-pr` 스킬로 PR 발행

### 6.2 상태 요약 (표)
| 항목 | 값 |
|---|---|
| 원본 이슈 | `#N` |
| 분할 이슈 | `#N1`, `#N2` (해당 시) |
| 브랜치 | `issue/...` |
| 플랜 | `.claude/plans/...md` |

---

## 가이드라인 (박스 요약)

- **읽기·계획은 자동, 쓰기는 승인**: Phase 1-4 자동 진행 OK. Phase 5 이슈·브랜치 생성은 반드시 사용자 승인 게이트 통과.
- **금지 행동**: 승인 없는 `gh issue create`, 승인 없는 `git push`, 부모 이슈 임의 close, 다른 담당자 이슈 수정.
- **허위 이슈 생성 금지**: 공개 이슈 본문을 확인할 수 없으면 진행하지 말고 사용자에게 안내.
- **Prefix 는 사용자 명시 후에만 사용**.
- **10파일 규칙은 타협 대상이 아님**: 예외는 사용자 사전 합의가 있는 foundation류 뿐. 초과 경계에서는 반드시 분할 제안.
- **플랜에 허위 체크 금지**: "수용 기준" 체크박스는 모두 빈 상태로 저장 (구현 후 raise-pr 단계에서 체크).
- **코드 구현·PR 발행은 범위 밖**: 플랜 작성 후 사용자 혹은 다른 스킬로 인계.

## 이 프로젝트에서의 트리거 예시

- `"plan-issue 8"` / `"#8 이슈 계획"`
- `"8번 이슈 분석해서 어떻게 할지 짜줘"`
- `"이슈 N번 작업할 브랜치 생성하고 구현 계획"` (브랜치 생성까지 명시 시 Phase 5 자연 진행)
- `"[JY] prefix로 8번 이슈 분할해서 플랜"` (prefix 명시)

## 다른 스킬과의 관계

- **후속**: `raise-pr` — 본 스킬이 만든 플랜을 따라 구현·커밋이 끝난 뒤 PR 발행. raise-pr 의 description 이 `plan-issue` 선행을 명시.
- **사용**: `manual-test` — UI 작업이 플랜에 포함되면 수용 기준 체크 시 참고로 언급 (본 스킬 자체는 시연하지 않음).
- **범위 밖**: `review`, `security-review`, `ultrareview` — 본 스킬은 플랜을 리뷰하지 않음.

---

## 실행 예시 — 이번 세션 #8 → #17/#18 분할 흐름

```
입력: "#8, 2.2 이슈에 대해 작업할 브랜치를 생성하고 구현 계획"

Phase 1: gh issue view 8 → "2.2 [Feature] JWT 인프라 + 로그인 API + 로그인 UI"
         본문의 "#13 kms-2.2.1", "#14 kms-2.2.2", "Depends on #7", "Parent: #3" 추출
         → #13, #14, #7, #3 본문도 조회

Phase 2: Explore 1개 (Spring Security 배선 지점 탐색)
         CLAUDE.md 규칙: 10파일/<300줄/TDD 필수 확인

Phase 3: 파일 수 산정 12~13 → 분할 필요
         AskUserQuestion: 타이트 / 분할 / 초과 → "분할" 선택
         prefix 확인: "[JY]" 수령

Phase 4: /Users/mac/.claude/plans/8-2-2-floating-eclipse.md 작성
         (사용자가 ~/.claude/plans 를 이미 열어둔 상태였던 특수 케이스;
          기본은 프로젝트 .claude/plans/8-jwt-login-feature.md)

Phase 5: 사용자 승인 후
         gh issue create [JY] 2.2.1 → #17
         gh issue create [JY] 2.2.2 → #18
         gh issue edit #8 (하위 링크 추가)
         gh issue comment #18 "Depends on #17"
         git checkout -b issue/17-jy-jwt-login-api origin/main

Phase 6: 인계 — "Phase 1 RED(JwtTokenProviderTest) 부터 착수 권장.
         구현·커밋 완료 후 raise-pr 호출"
```
