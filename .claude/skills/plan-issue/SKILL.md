---
name: plan-issue
description: GitHub 이슈 링크를 받아 해당 이슈의 본문·의존성·라벨을 확인하고, 이 프로젝트(Spring Boot + CLAUDE.md TDD 규칙)에 맞는 구현 계획을 세워 사용자 승인까지 받는 워크플로우. 사용자가 "이 이슈 계획 세워줘", "plan-issue", "이슈 분석해서 계획", "이거 어떻게 할지 짜줘" 등으로 GitHub 이슈 URL과 함께 요청할 때 호출한다. **이슈 URL은 필수 인자** — URL이 주어지지 않으면 스킬을 중단하고 사용자에게 재호출을 안내한다. 이 스킬의 범위는 플랜 문서 작성과 승인까지이며, 브랜치 생성·TDD 구현·PR 생성은 범위 밖(`raise-pr` 또는 직접 구현 흐름).
argument-hint: <github-issue-url>
---

# plan-issue — GitHub 이슈 → 구현 계획 수립

## 수신한 인자

**ARGUMENTS:** `$ARGUMENTS`

## 🛑 사전 검증 — 인자 필수 (가장 먼저 실행)

이 스킬은 **GitHub 이슈 URL 1개**를 필수 인자로 요구한다. 다른 모든 단계 **이전에** 위 ARGUMENTS 값을 검사한다.

**중단 조건 (해당되면 즉시 작업을 멈추고 사용자에게 재호출 안내 후 return)**
1. `$ARGUMENTS`가 비어 있음 (문자열 `"$ARGUMENTS"`가 그대로 남아 있거나 공백만).
2. `$ARGUMENTS`에 `github.com/<owner>/<repo>/issues/<정수>` 패턴이 없음.
3. URL은 있으나 `issues` 경로가 아님 (예: `/pull/`, `/discussions/`, 루트 `/`).

**중단 시 정확한 사용자 안내 (다른 행동 금지)**
> plan-issue 스킬은 GitHub 이슈 URL이 반드시 필요하다. 예:
> `/plan-issue https://github.com/marcosdeseul/dihisoft-claude-session/issues/8`
>
> 이슈 URL과 함께 다시 호출해주면 계획을 세운다.

이 안내 외에 `gh` 호출, `Read`, `Bash`, Explore 에이전트 호출 등 **어떤 도구도 사용하지 않는다**. URL이 있을 때만 Phase 1로 진입한다.

---

이 프로젝트의 고유 규칙(CLAUDE.md TDD · 파일 ≤10 · 커버리지 95% · 한국어 테스트명) 위에서 **이슈를 읽어 플랜 문서로 소화**하고, 사용자 승인까지 받는다. 승인 이후 구현은 사용자 또는 별도 흐름으로 이어지며, 이 스킬은 그 전 단계까지만 책임진다.

## 전제
- `gh` CLI 인증 완료 (`gh auth status`로 사전 확인 가능).
- 현재 디렉토리가 대상 레포의 로컬 클론.
- `main`(또는 기본 브랜치) 최신 상태가 바람직 — 스킬은 자동 pull하지 않음. 필요하면 사용자에게 확인.

## 필수 인자 (재확인)
**이슈 URL.** 형식 예: `https://github.com/<owner>/<repo>/issues/<n>`.

맨 위 "🛑 사전 검증"에서 이미 걸러졌어야 한다. 만약 여기까지 왔는데 URL을 아직 확보하지 못했다면 **즉시 중단**. `AskUserQuestion`으로 받지 말 것 — 스킬 호출 단계에서 강제해야 일관성이 유지된다.

---

## Phase 1 — 이슈 수신·파싱

### 1.1 URL 파싱
```bash
# URL에서 owner/repo/number 추출 (지연 평가 금지, 즉시 실행)
# https://github.com/foo/bar/issues/8 → owner=foo, repo=bar, num=8
```
`gh issue view` 호출 시 `--repo foo/bar` 형식 사용. WebFetch는 인증 한계로 실패 가능성 있으므로 **항상 `gh` 우선**.

### 1.2 이슈 본문 로드
```bash
gh issue view <num> --repo <owner>/<repo>
gh issue view <num> --repo <owner>/<repo> --json body,labels,state,assignees,milestone --jq .
```
확보할 항목:
- 제목·본문·상태(open/closed)
- 라벨(`area:*`, `priority:*`, `type:*`)
- 수용 기준 / 체크리스트 / Edge case / 의존성(`Depends on #X`) / 부모(`Parent: #Y`)
- 참고 링크(README, CLAUDE.md 등)

이슈가 closed면 **즉시 사용자에게 확인** — 이미 처리된 건인지, 재오픈 의도인지.

### 1.3 의존성 점검 — 블로킹 사전 발견
이슈 본문에 `Depends on #X` 나 `Blocked by #X`가 있으면:
```bash
gh issue view <x> --repo <owner>/<repo> --json state,closedAt --jq '.state + " @ " + (.closedAt // "null")'
```
미해결 의존성이 있으면 사용자에게 보고하고 진행 여부 확인(블로킹 해제 후 재시도 권장).

부모 에픽(`Parent: #Y`)은 컨텍스트 확보용으로 본문만 확인.

---

## Phase 2 — 코드베이스 탐색 (Explore 에이전트 병렬)

### 2.1 스코프 추정
이슈 본문의 "범위 / 결과물" · "수용 기준" · 파일 언급을 읽고 건드릴 영역을 추린다. 예:
- "POST /api/auth/login + 필터 + UI" → auth 모듈, SecurityConfig, Thymeleaf templates
- "게시글 생성 REST" → posts 모듈, BaseEntity, 페이징 유틸

### 2.2 Explore 에이전트 호출 (최대 3개 병렬)
스코프가 불확실하거나 여러 레이어가 엮여 있으면 **한 메시지에서 여러 Agent를 동시 실행**한다. 각 에이전트에 **구체적 파일 경로·키워드**를 전달하고 보고서 형식을 지정.

단일 에이전트로 충분한 경우:
- 이슈가 좁은 영역만 건드림 (예: 단일 엔드포인트 추가)
- 기존 유사 기능을 1:1 미러링 (예: 회원가입 → 로그인)

3개로 병렬화하는 경우:
- 에이전트 A — 대상 모듈 기존 구현 탐색(패턴 확보)
- 에이전트 B — 연관 설정/보안/공통 유틸 탐색(재사용 대상)
- 에이전트 C — 기존 테스트 패턴 탐색(`@Nested`, `@Tag`, H2/TestPropertySource 주의점)

보고서에 반드시 포함 요청:
- 패키지/네이밍 컨벤션
- 재사용 가능한 유틸·DTO·핸들러 **절대 경로**
- 기존 테스트 구조(AAA, 한국어 메서드명, H2 설정, 프로필)
- 빌드/의존성 상태(필요 라이브러리 이미 있는지)

### 2.3 Critical 파일 직접 읽기
에이전트 보고서의 신뢰도를 올리기 위해, **플랜에 인용할 핵심 파일 3~5개**는 `Read`로 직접 확인한다(환각 방지). 특히:
- `CLAUDE.md` (프로젝트 규칙)
- `build.gradle.kts` (의존성 현재 상태)
- `src/main/resources/application.yml` + `application-test.yml`
- 이슈가 건드릴 기존 Controller·Service 1~2개

---

## Phase 3 — 제약·선호 반영

### 3.1 CLAUDE.md 규칙
- **TDD**: RED → GREEN → REFACTOR 사이클 필수, 커밋도 분리.
- **Edge case 체크리스트**: 입력검증·인증/권한·리소스상태·경계값·형식·부수효과·동시성.
- **수정 파일 ≤ 10**: 프로덕션·테스트·설정 모두 합산.
- **파일당 < 300줄**: 초과 가능성이면 책임 분리 선계획.
- **테스트명**: 한국어 `given-when-then` / `should_...` / `<조건>_<기대>`.
- **통합 우선 · 모킹 최소**: Controller는 `@SpringBootTest + @AutoConfigureMockMvc`, Repository는 H2 실물.

### 3.2 memory 확인
`/Users/bj/.claude/projects/<encoded-path>/memory/MEMORY.md`를 읽고 이 이슈와 관련된 사용자 선호를 적용한다. 예:
- `feedback_scope_split.md` — **"파일 수 초과 시 하위 이슈로 쪼개지 말고 한 브랜치에서 Phase 커밋으로 진행"** (이슈 8 계획 시 지시된 선호, CLAUDE.md ≤10 규칙보다 우선).

충돌 시: **memory의 사용자 선호 > 프로젝트 규칙**. 대신 PR 본문에 "의도적 예외 · 사유" 명시.

### 3.3 파일 수 추정 → 구조 결정
예상 수정 파일 수를 세어보고 다음 중 하나를 고른다:

| 예상 파일 수 | 구조 |
|---|---|
| ≤10 | 단일 Phase, 일반 TDD 사이클 |
| 11~14 | 단일 브랜치 + Phase A/B 커밋 시리즈 (기본 선호) |
| ≥15 | **사용자에게 확인**: 하위 이슈 분할 vs 더 큰 Phase 시리즈 |

---

## Phase 4 — 명료화 질문 (AskUserQuestion)

다음과 같은 **판단이 필요한 항목**이 있으면 선제적으로 `AskUserQuestion`으로 묻는다. 플랜 승인 단계(`ExitPlanMode`)에서가 아니라 **지금 여기서**.

자주 물어야 하는 항목:
- 로그인/세션 전략 (쿠키 vs 헤더 vs 둘 다)
- 성공 후 리다이렉트 경로
- 에러 응답 shape (기존 `ErrorResponse` 재사용 여부)
- 라이브러리 선택지 (JJWT vs Nimbus, Flyway vs Liquibase)
- 테스트 커버리지 우선순위 (unit만 vs 통합+Playwright까지)

"이 계획 괜찮아?" 같은 승인 질문은 `AskUserQuestion`으로 **하지 말 것** — 그건 `ExitPlanMode`의 역할이다.

---

## Phase 5 — 플랜 파일 작성

### 5.1 위치
- 플랜 모드가 활성이면 시스템이 제공한 경로(`/Users/bj/.claude/plans/<encoded>.md`)를 그대로 사용.
- 비활성이면 `/tmp/plan-<issue-num>.md` 임시 경로. 이후 사용자에게 승인 여부 명시적으로 묻는다.

### 5.2 플랜 문서 골격
```markdown
# Issue #<num> — <제목>

## Context
<왜 이 변경이 필요한가. 1~3문단. 이슈에서 인용>
<memory/CLAUDE.md에서 적용되는 선호·제약 요약>

## 선행 작업: 브랜치 생성
\`\`\`bash
git switch -c issue/<num>-<short-slug>
\`\`\`
- 네이밍: 기존 패턴 준수 (`issue/7-signup-with-ui` 등)
- 분기 기준: main 최신 커밋 `<hash>`

## Phase A — <이름>

### 신규 프로덕션 (N)
1. `src/main/java/...` — <역할 1~2줄>
2. ...

### 수정 프로덕션 (M)
3. `src/main/java/...` — <변경 요약>

### 신규/수정 테스트 (K)
- `src/test/java/.../XxxTest.java` — <@Nested 구조, 커버할 케이스>

### TDD 커밋 시리즈
\`\`\`
test: <기능> — <케이스> RED 추가
feat: <기능> 구현 — GREEN
refactor: (선택) <무엇을 개선>
\`\`\`

## Phase B — <이름 · 해당 시>
... (동일 구조)

## Edge Case 체크리스트 (RED 설계)
- [x] 입력: null/빈/경계값
- [x] 인증: 토큰 없음/만료/변조
- [x] 리소스: 존재하지 않음/중복
- [x] 형식: 잘못된 JSON/누락 필드
- [x] 부수효과: 트랜잭션 롤백/DB 오염 없음

## 참고 파일 (재사용 대상)
- `<path>` — <어떤 패턴 재사용>
- ...

## application.yml / 설정 영향
<수정 필요/불필요 명시. 이미 있는 키가 있으면 언급>

## 최종 파일 카운트
- 신규: X / 수정: Y / 합계: Z (CLAUDE.md ≤10 <준수|예외사유>)

## 검증 절차
\`\`\`bash
./gradlew test                              # 자동 테스트 GREEN
./gradlew jacocoTestCoverageVerification    # 95% 게이트
./gradlew test -PincludePlaywright          # (해당 시) 브라우저 스모크
git diff --stat main...HEAD                 # 파일 수 확인
\`\`\`
수동 스모크: <UI 경로·curl 예시 1~2줄>

## 다음 스킬
- 구현 완료 후 `raise-pr` 스킬로 PR 생성
- UI 포함 변경이면 `manual-test` 스킬로 증적 확보 권장
```

### 5.3 불필요한 것 넣지 않기
- 모든 대안 나열 금지 — **추천안 하나**만.
- 미래 가능성·hypothetical 피하기.
- "이 계획이 괜찮다면..." 같은 조건문 금지.

---

## Phase 6 — 승인

### 6.1 플랜 모드 활성인 경우
`ExitPlanMode` 호출 — 플랜 파일 내용이 사용자 UI에 표시되고 승인 여부를 묻는다. 동일 턴 내 추가 질문·추가 편집 금지. `AskUserQuestion`으로 재질문 금지.

### 6.2 플랜 모드 비활성인 경우
플랜 파일 경로를 사용자에게 알리고, 구현 시작 여부를 **명시적으로 묻는다**:
> 계획은 `/tmp/plan-<n>.md`에 썼다. 내용 검토 후 "진행" 라고 알려주면 브랜치 생성부터 들어가고, 조정할 부분이 있으면 지적해줘.

승인 떨어지기 전까지 **어떤 git 상태 변경 동작도 금지** (브랜치 생성 포함).

---

## 가이드라인 (박스 요약)

- **URL 필수**: 이슈 URL 없이 절대 추측·기본값 진행 금지. 없으면 `AskUserQuestion`으로 받는다.
- **`gh` 우선**: GitHub 데이터는 WebFetch가 아닌 `gh issue view`로. 인증·rate-limit 안정.
- **병렬 Explore**: 스코프가 넓으면 한 메시지에서 여러 Agent 호출. 각 에이전트에 파일 경로와 구체 질문.
- **memory 확인**: 파일 수 초과 시 선호(Phase 커밋 vs 하위 이슈) · 사용자 선호 등 기존 메모리 먼저 확인.
- **CLAUDE.md 규칙 반영**: TDD 사이클 / 파일 ≤10 / 커버리지 95% / Edge case 체크리스트.
- **질문은 Phase 4에서만**: 판단 필요한 항목을 미리 해소. 승인은 `ExitPlanMode`로.
- **브랜치 생성 금지**: 이 스킬은 플랜까지만. 브랜치·커밋·push 등은 승인 뒤 단계.

---

## 다른 스킬과의 관계

- **후행**: `raise-pr` — 이 스킬로 승인받은 플랜을 기반으로 TDD 사이클을 돌려 커밋을 쌓은 뒤, 그 브랜치에서 PR 라이프사이클을 처리. raise-pr은 "구현 완료된 브랜치"를 전제로 한다.
- **보조**: `manual-test` — UI 포함 변경이면 구현 후 사용.
- **범위 밖**: 실제 구현(TDD 사이클 실행), 브랜치 생성, PR 생성 — 모두 승인 이후 흐름.

---

## 실행 예시 — 이번 세션 이슈 #8 흐름

```
1) 사용자: "https://github.com/marcosdeseul/dihisoft-claude-session/issues/8 계획"
2) gh issue view 8 --repo marcosdeseul/dihisoft-claude-session
   → 본문: JWT 인프라 + 로그인 REST + 로그인 UI. Parent #3, Depends on #7(closed)
3) Explore 에이전트 1개 호출 (auth 모듈 전반 — 단일로 충분)
   → SecurityConfig stateless/JSON 401 핸들러 / AuthController dual 엔드포인트 /
     signup.html 구조 / JJWT 0.12.6 이미 의존성 있음 / app.jwt.* 이미 application.yml 등록
4) Critical 파일 직접 읽기: CLAUDE.md, build.gradle.kts, SecurityConfig, AuthController, application.yml
5) 파일 수 추정 → 13개 (≤10 초과) → memory의 feedback_scope_split.md 확인 →
   Phase A/B 커밋 시리즈로 진행 (사용자 선호 · 의도적 예외)
6) AskUserQuestion:
   - 쪼개기 전략(Phase 커밋 / 단일 PR + 축소 / 예외 허용)
   - 로그인 성공 후 UX(리다이렉트 + 쿠키 / 홈 / JSON만)
7) 플랜 파일 작성: Phase A(JWT 인프라 + REST 로그인) / Phase B(UI + Playwright)
8) ExitPlanMode → 사용자 승인 → 이 스킬 종료
9) (범위 밖) 브랜치 생성 → RED/GREEN 커밋 × N → raise-pr
```
