---
name: plan-issue
description: GitHub 이슈 번호/URL을 받아 본문·의존성·코드베이스를 분석하고, 플랜 모드로 진입해 CLAUDE.md 제약(≤10파일·<300줄·TDD RED 선제 설계)에 맞춘 구현 플랜을 작성·승인받은 뒤 기능 브랜치를 생성하는 워크플로우. 사용자가 "이슈 N 진행", "/plan-issue", "이슈부터 시작", "이 이슈로 플랜", "이슈 기반 구현 계획" 등으로 요청할 때 호출한다. 플랜 승인 + 브랜치 체크아웃 직후 종료 — 실제 TDD 구현은 범위 밖(일반 모드에서 이어감). 후속 스킬: manual-test(UI), raise-pr(PR 생성).
---

# plan-issue — 이슈 → 플랜 → 브랜치

이 프로젝트(Spring Boot + Gradle + JaCoCo 95% 게이트 + CLAUDE.md TDD 규칙)에 맞게, **이슈를 받아 구현 플랜을 세우고 기능 브랜치까지 만드는 앞단 워크플로우**를 일관 처리한다.

## 전제
- `gh` CLI 인증 완료 (`gh auth status` → Logged in).
- 로컬 `main` 이 `origin/main` 과 동기화 가능한 상태 (미커밋 변경 없음).
- Claude Code 가 플랜 모드로 진입 가능 (이 스킬 자체가 플랜 모드를 사용).
- `README.md`, `CLAUDE.md` 를 읽은 적 있거나, Phase 2에서 재확인 예정.

아직 이슈가 없다면 먼저 `gh issue create` 또는 리포에서 이슈를 만든 뒤 호출.

## 언제 사용하는가
- 새 이슈를 막 받았고 아직 브랜치/커밋이 없을 때
- 구현 착수 전에 파일 수·TDD edge case·재사용 자산을 설계로 먼저 확정하고 싶을 때
- 의존 이슈가 복잡해 착수 전 의존 관계·선결 조건을 명확히 할 필요가 있을 때

사용하지 말 것: 이미 브랜치에 커밋이 있음(그땐 `raise-pr` 로 직행). 단순 오타·한 줄 수정(바로 고치면 됨).

## Phase 1 — 이슈 수집

```bash
gh issue view <N>                # 본문·체크박스·수용기준
gh issue view <N> --json labels,assignees,milestone
```

- **부모/의존 추적**: 본문에 `Parent: #X`, `Depends on #Y` 나오면 각각 `gh issue view X|Y` 로 읽는다.
- **의존 이슈가 OPEN 이면** 바로 착수 가능성 재검토. `AskUserQuestion` 으로 선결/대체/우회 협의.
- **체크박스 수용 기준**은 TDD RED 설계의 원재료 — 반드시 스캔해둔다.
- 범위 밖 항목(refresh token 등)이 있으면 플랜에 "범위 밖" 섹션으로 명시.

## Phase 2 — 코드베이스 정찰

**필수 문서 재확인** (스킬 호출 맥락이 이미 맥락에 있으면 스킵):
- `README.md` — 스펙, 도메인 모델, API 스펙, 에러 규약
- `CLAUDE.md` — TDD 규칙, ≤10파일, <300줄, 테스트 구조 기준

**재사용 자산 식별** — Explore 서브에이전트 1~3개 병렬 권장:
- 이슈 도메인과 같은 패키지의 기존 클래스/테스트
- Repository 메서드 (예: `UserRepository.findByUsername`)
- Security/Config 빈 (예: `BCryptPasswordEncoder`, `RestAuthenticationEntryPoint`)
- Error 처리 (`GlobalExceptionHandler`, `ErrorResponse.of(...)`)
- DTO·Controller 패턴 (예: `SignupRequest`, `AuthController`)
- 테스트 패턴 (`@SpringBootTest` + `@Nested(RestApi/View)`, 한국어 테스트명)
- 빌드/설정 (`build.gradle.kts` 이미 선언된 의존성, `application.yml` 이미 있는 키)

**기존 설정·의존성 중복 선언을 방지**한다 — 이미 있는 건 "재사용 자산" 으로 플랜에 명시.

## Phase 3 — 플랜 모드 진입

플랜 파일(`.claude/plans/<session-id>.md`) 에 다음 구조로 작성:

1. **Context** — 왜 이 변경이 필요한가, 배경과 의도한 결과.
2. **선결 조건** — 의존 이슈 상태, 이미 존재하는 빈/설정/의존성.
3. **파일 계획 (정확히 N개)** — 신규/수정 합쳐 카운트. **10 초과 예상 시 하위 이슈(`x.1/x.2`) 분할 제안 후 중단**.
4. **신규 파일 예상 라인 수** — 300 초과 예상이면 책임 분리 설계 사전 반영.
5. **TDD RED edge case 체크리스트** — CLAUDE.md §Edge Case 체크리스트 적용:
   - 입력 검증 (null/빈/공백/경계값/타입)
   - 인증·권한 (토큰 없음/만료/변조/본인 아님)
   - 리소스 상태 (없음/중복/삭제됨)
   - 형식 (잘못된 JSON/누락·추가 필드)
   - 부수효과 (트랜잭션/쿠키/리다이렉트)
6. **재사용 자산 경로** — Phase 2 에서 찾은 파일:line 링크.
7. **커밋 계획** — RED/GREEN/REFACTOR 분리 원칙으로 3~5개.
8. **수동 검증** — UI 변경 있으면 `manual-test` 단계 포함(시나리오·스크린샷 이름 사전 지정).
9. **최종 검증 명령** — `./gradlew clean build`, `git diff --stat main...HEAD`.

**모호점은 `AskUserQuestion` 으로 해소** — 예: 토큰 전달 방식(바디 vs 쿠키), DB 마이그레이션 정책, UI 포함 여부, 문자 집합(숫자-only vs 알파벳 포함) 등. "Is plan ok?" 류 질문은 ExitPlanMode 로 대체.

## Phase 4 — 플랜 승인 + 브랜치 생성

1. `ExitPlanMode` 호출 → 사용자 승인 대기.
2. 승인되면 브랜치를 만든다. 네이밍 규칙은 기존 선례(`issue/7-signup-with-ui`, `issue/8-jwt-login`) 를 따름:

   ```bash
   git checkout main
   git pull --ff-only
   git checkout -b issue/<N>-<short-slug>
   ```

3. 여기까지가 이 스킬의 범위 — 이후 TDD 구현(`test:` RED → `feat:` GREEN → `refactor:`)은 일반 모드에서 이어간다.

**미승인 시**: 사용자 피드백 반영해 Phase 3 으로 돌아가 플랜 갱신 후 재승인. 브랜치 먼저 만들지 않음.

## 프로젝트 고유 규칙 체크리스트 (CLAUDE.md 축약)

- [ ] **이슈당 수정 파일 ≤ 10** (프로덕션+테스트+설정 모두 합산, wrapper 자동생성 제외)
- [ ] **신규 파일 < 300줄**. 초과 예상이면 책임 분리
- [ ] **TDD**: RED 먼저 작성 → 반드시 실패 확인 → GREEN → 리팩터. RED/GREEN 최소 분리 커밋
- [ ] **통합 테스트 우선**: Controller 는 `@SpringBootTest` + `MockMvc`. Repository 는 H2 인메모리. 외부 I/O 만 모킹
- [ ] **한국어 테스트명**: `given-when-then` 또는 `should_...` — 예: `signup_중복된_username이면_400을_반환한다`
- [ ] **Edge case 를 RED 에서 선제 설계** — "나중에 추가" 금지

## 다른 스킬과의 관계

- **선행**: 없음 (플로우의 시작)
- **후속**: 
  - `manual-test` — 구현 중/후에 UI 포함 시 시연·스크린샷 증적
  - `raise-pr` — 구현·커밋 완료 후 push → PR → CI → 리뷰 대기
- **범위 밖**: 실제 코드 작성(일반 모드), 로컬 테스트 실행, PR 생성

## 가이드라인 (박스 요약)

- **정찰 먼저, 설계 다음**: Phase 2 없이 바로 Phase 3 가면 이미 있는 자산을 재발명하게 됨
- **10파일 제약은 제안의 한계선**: 넘을 것 같으면 플랜을 그대로 올리지 말고 분할 제안
- **Edge case 는 RED 의 목록**: Phase 3 체크리스트를 그대로 `@Nested` 테스트 이름으로 번역하면 충분
- **승인 없이 브랜치 금지**: 사용자가 플랜을 바꿀 수 있으니 브랜치 네이밍이 맞지 않게 될 수 있음
- **머지 금지**: `gh pr merge` 는 raise-pr 단계에서도 자동 실행 금지. 이 스킬은 아예 PR 전이라 해당 없음

## 실행 예시 — 이번 세션의 #8 실제 기록

```
Phase 1: gh issue view 8 → 부모 #3(epic) · 의존 #7(MERGED) 확인
         본문의 "상세는 2.1 머지 후 별도 계획으로 확정" 단서 포착
Phase 2: README.md(API 스펙) + CLAUDE.md(TDD·10파일) 재확인
         기존 user/·config/·common/ 디렉토리 탐색
         재사용 자산 확정:
         - UserRepository.findByUsername (이미 존재)
         - BCryptPasswordEncoder 빈 (SecurityConfig:27)
         - RestAuthenticationEntryPoint (401 JSON 포맷 완비)
         - GlobalExceptionHandler(IllegalArgumentException → 400, validation → 400)
         - application.yml 의 app.jwt.secret·expiration-ms 이미 설정됨
         - build.gradle.kts 에 jjwt 0.12.6 이미 선언
Phase 3: 플랜 작성 — 파일 정확히 10개
         신규 5(JwtTokenProvider·JwtAuthenticationFilter·CustomUserDetailsService·LoginRequest·TokenResponse)
         수정 3(SecurityConfig·AuthController·AuthService)
         리소스 1(login.html) + 테스트 1(LoginControllerTest)
         Filter 테스트용 보호 엔드포인트는 LoginControllerTest 내 @TestConfiguration 으로 내재화 → 파일수 10 유지
         Edge case: 입력검증(null/빈), 자격실패(잘못된pw/없는user), 토큰(없음/Bearer누락/변조/만료/다른서명)
         AskUserQuestion 없이 진행 (스펙 충분)
Phase 4: ExitPlanMode → 승인 → git checkout -b issue/8-jwt-login
         스킬 종료. 이후 TDD(RED 커밋 → GREEN 커밋)는 일반 모드에서 계속
```

이 기록을 따라가면 #8과 유사한 난이도 이슈 대부분이 동일 패턴으로 흘러간다.
