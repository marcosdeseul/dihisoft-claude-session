---
name: plan-issue
description: GitHub 이슈 번호를 받아 **작업 착수 전 계획**을 수립한다. 이슈·의존 이슈·관련 코드를 탐색하고, CLAUDE.md의 작업 단위 제약(이슈당 ≤10 파일, 파일당 <300줄, TDD RED 선행, 95% 커버리지)에 맞는지 예측 파일 수로 검증한 뒤, 필요 시 `x.1/x.2` 하위 이슈로 분할 제안·생성한다. 마지막에 plan 파일을 작성하고 `ExitPlanMode` 로 승인을 받는다. 사용자가 "이슈 #N 계획", "plan issue N", "N번 이슈 착수 계획", "이슈 분석해서 어떻게 할지 알려줘" 등으로 요청할 때 호출한다. 구현·커밋·PR은 범위 밖(`raise-pr` 또는 자유 구현 흐름).
---

# plan-issue — GitHub 이슈 착수 전 계획 수립

이 프로젝트(Spring Boot + Gradle + JaCoCo 95% + TDD 필수)에서 **이슈 하나를 받아 실행 가능한 계획**을 만들어내는 워크플로우. 구현 첫 줄을 쓰기 **전에** 돌린다.

## 언제 사용하는가
- 방금 할당된 이슈의 범위·파일·테스트 전략을 사용자와 확정하고 싶을 때
- 이슈 본문만 보면 스코프가 ≤10 파일 규칙을 넘을지 확신이 안 설 때
- 관련 코드·재사용 가능한 유틸·기존 테스트 관용을 먼저 파악해야 할 때
- TDD 사이클의 RED 케이스를 이슈별 edge case 체크리스트로 전환해야 할 때

사용하지 말 것:
- 단순 오타·한 줄 수정 (바로 구현)
- 이미 구현이 끝나 PR만 올리면 되는 경우 (`raise-pr`)
- 이슈 없이 자유 리팩터 (context를 읽어 계획 → 바로 구현)

## 전제
- `gh` CLI 인증 완료, 원격 `origin` 설정됨
- `CLAUDE.md` 가 리포 루트에 있고 작업 단위·TDD 규칙이 최신
- 계획 파일 경로는 harness가 지정하는 `/Users/<user>/.claude/plans/<slug>.md` 를 그대로 사용 (플랜 모드 진입 시 제공됨)

## 워크플로우

### Phase 1 — 이슈 읽기 (사실 수집)
```bash
gh issue view <N> --json title,body,labels,state
```
의존/상위 이슈도 함께 읽는다 (이슈 본문에서 `#M` 패턴 추출):
- `Depends on #M` → 머지 여부 확인 (`gh issue view M --json state`)
- `Parent: #M` → 에픽의 다른 서브이슈와 범위 중복이 없는지 확인
- `Closes #M` 또는 역링크 → 이미 해결된 부분 구분

**확인 포인트**
- 목적·범위·수용 기준 체크박스
- 명시된 파일 목록 (없으면 본문에서 추정)
- 외부 스펙 참조 (README `§N`, 설계 문서 등)

### Phase 2 — 코드베이스 탐색 (재사용 우선)
거의 항상 병렬로 **1~3개 Explore 에이전트** 를 동시에 띄운다 (한 메시지에 여러 Agent 호출).

**분할 기준 예시**
- 에이전트 1 — 기존 도메인/서비스/컨트롤러 구조 (재사용 대상 탐색)
- 에이전트 2 — 기존 테스트 관용 (setUp, @Nested 구조, 한국어 테스트명)
- 에이전트 3 — 관련 설정·인프라 (SecurityConfig, application.yml, build.gradle.kts)

에이전트 프롬프트에 반드시 포함:
- 읽어야 할 **실제 파일 경로** (추측 금지)
- 보고 형식 (500~700단어, 경로:라인 인용, "재사용 가능 vs 신규 추가" 섹션)
- "코드 내용을 인용하며 정리" 강제

탐색이 끝나면 핵심 2~3개 파일을 직접 `Read` 로 재확인 (에이전트 보고서의 요약 정확도 검증).

### Phase 3 — 파일 수 예측 + 분할 판단
**예측 리스트** (프로덕션 신규/수정, 테스트 신규/수정, 리소스·템플릿, 빌드 파일을 **모두 합산**):

| 구분 | 파일 | 신규/수정 |
|---|---|---|
| prod | `...` | new |
| test | `...` | new |
| ... | ... | ... |

**의사결정 기준 (CLAUDE.md §작업 단위 제약)**
- 예측 ≤ 10 → 단일 이슈로 진행
- 예측 > 10 → **분할**:
  - 분할 기준은 "독립적으로 TDD 한 사이클이 성립하는 feature slice" (CLAUDE.md 명시)
  - 보통 `2.0` → `2.1`/`2.2`, 또는 `2.2` → `2.2.1`/`2.2.2` 식 네이밍
  - **중요**: 원본 이슈는 `gh issue edit`로 수정하지 **말고**, 새 하위 이슈 두 개를 `gh issue create`로 추가 (사용자 선호 패턴 — 세션 #8 사례)
  - 새 이슈 title에 사용자가 지정한 prefix(예: `[SHJ]`)를 반드시 포함 — 지정 없으면 짧은 질문
  - 새 이슈 body에 `원본 스펙: #N` 링크 + `Depends on: #A` 체인

**AskUserQuestion 로 확정 (≥11개일 때만)**
- 옵션 A: 표준 분할 `x.1/x.2` (추천, CLAUDE.md 규칙 준수)
- 옵션 B: 단일 PR, 범위 축소 (예: Playwright 제외) 으로 10개 이하
- 옵션 C: 단일 PR, 11~12개 수용 (규칙 일시 완화)

질문 후 사용자 지시에 따름 — 분할 이슈의 prefix·브랜치 suffix 관례도 이 단계에서 함께 확인.

### Phase 4 — Plan 에이전트 (선택)
탐색 결과가 복잡하거나 대안이 여러 개면 **1개의 Plan 에이전트**를 띄워 구현 순서·RED 케이스를 점검 받는다.
간단한 slice(DTO + 컨트롤러 하나)라면 생략 가능. 경험상 이 프로젝트의 각 서브이슈(≈7~8 파일)는 Plan 에이전트 없이 직접 작성해도 충분.

### Phase 5 — Plan 파일 작성
harness가 안내한 경로(`/Users/<user>/.claude/plans/<slug>.md`)에 아래 구조로 작성. 빈 파일에서 시작해 **증분 저장** — 긴 본문을 한 번에 쏟지 말고 `Write` → `Edit` 로 다듬는다.

**필수 섹션**
1. **Context** — 왜 이 변경인가 (이슈 목적, 선행 이슈, 결과물 한 줄씩)
2. **분할 결정** (분할했을 때만) — 어느 이슈를 새로 만들고 원본은 어떻게 다루는지
3. **브랜치·이슈 운영** — `git checkout -b feature/<name>-<suffix>`, `gh issue create ...`, PR body의 `Closes #N`·`Depends on #M` 규칙
4. **서브이슈별 범위 / 파일 목록** — 표로 7~10개, `new|modified` 구분
5. **TDD 사이클** — 각 테스트 클래스의 RED 케이스를 한국어 메서드명 예시로 나열 (CLAUDE.md §Edge Case 체크리스트 경유)
6. **재사용 체크리스트** — 기존 클래스/빈/템플릿을 `경로:라인` 표기로 명시 (신규 작성 전 확인용)
7. **검증 (E2E)** — `./gradlew clean test`, `jacocoTestCoverageVerification`, 수동 시연 스킬(`manual-test`) 트리거 조건
8. **산출물 요약** — 단계별 한 줄 표

**피할 것**
- 대안 나열 — 추천 접근 하나만
- "더 고민 후 결정" 류의 미결 — 결정을 사용자에게 바로 돌려서 확정
- 코드 덩어리 붙여넣기 — 경로·시그니처·핵심 한 줄만

### Phase 6 — AskUserQuestion 로 마지막 모호점 해소
이 시점까지도 남은 설계 선택지(예: "LoginRequest에 길이 제약 둘지 말지", "login 성공 시 쿠키 저장 vs 리다이렉트만")는 `AskUserQuestion` 로 2~4개 보기 중 선택 받음. 각 옵션의 trade-off를 한 줄씩.

Plan 파일 body나 체크박스 정합성을 "맞나요?" 류로 묻지 말 것 — 그건 `ExitPlanMode`가 할 일.

### Phase 7 — ExitPlanMode
`ExitPlanMode` 호출. `allowedPrompts` 에는 실행 단계에서 필요한 Bash 카테고리를 명시:
- `"run gh CLI to create new GitHub issues (do not modify original)"`
- `"create and checkout git branches from main with <suffix> suffix"`
- `"run ./gradlew test and jacoco tasks"`
- `"git add/commit for TDD RED and GREEN steps"`
- `"git push and gh pr create"`

사용자가 수정 요청하면 plan 파일을 `Edit` 로 반영 후 **다시** `ExitPlanMode` 호출. 대화에서 재확인 요구 금지.

## 가이드라인

- **Phase 2 병렬화는 필수가 아니라 기본값**: 병렬 Explore는 컨텍스트 보호 + 탐색 속도 양쪽에서 유리. 단일 파일 수정처럼 스코프가 좁은 태스크만 1개 에이전트 또는 직접 `Read`.
- **이슈 분할은 항상 새 이슈 생성**: 원본 이슈의 title/body를 수정하지 않는 것이 이 프로젝트 사용자 선호(세션 #8 피드백). 새 이슈 title에 prefix(`[SHJ]` 등) 관례 포함.
- **≤10 파일은 하드 제약**: 착수 전에 반드시 세어본다. 예측 실패로 11개가 되면 즉시 멈추고 사용자에게 분할 동의 요청.
- **재사용 확인은 Phase 2 강제 산출물**: plan 파일에 "재사용 체크리스트" 섹션이 없으면 이 스킬을 잘못 돌린 것. 중복 구현은 리뷰어가 가장 싫어한다.
- **TDD 사이클은 이슈 체크리스트로 변환**: CLAUDE.md Edge Case 체크리스트의 7항목(입력 검증/인증 권한/리소스 상태/경계값/형식/부수효과/동시성)을 **이슈별로** 대응되는 RED 케이스로 풀어 적는다. 스킬이 추천하는 메서드명 스타일: `given_when_then` 대신 한국어 서술형 (`~이면_~한다`).
- **플랜 파일은 단 하나**: harness가 지정한 경로만 편집. 다른 md 파일 만들지 말 것.
- **답이 없는 계획은 계획이 아니다**: Phase 6 `AskUserQuestion`으로 모호점 줄이고, 결정한 하나의 방향만 Phase 5 plan 파일에 서술.

## 금지 행동 (사용자 승인 없이 금지)
- `gh issue edit <원본>` — 원본 이슈 title/body/상태 변경
- `git checkout -b` 등 브랜치 생성 (플랜 모드 탈출 **전**)
- `gh issue create` — 플랜 승인 **전**
- 구현 파일 작성 — 플랜 승인 **전** (plan 파일만 편집 허용)

## 이 프로젝트에서 자주 부딪히는 패턴

- **이슈 `2.0` → `2.1`/`2.2` 분할**: 회원가입(#7)과 JWT+로그인(#8)처럼 도메인이 독립 slice로 쪼개짐
- **이슈 `2.2` → `2.2.1`/`2.2.2` 분할**: JWT 인프라 slice와 로그인 API/UI slice — 파일 예산 때문이지만 TDD 사이클도 독립적으로 성립
- **브랜치 네이밍**: `feature/<short-name>-<user-suffix>` (예: `feature/jwt-infra-shj`) — suffix는 사용자 지정, 이 스킬에서 모르면 질문
- **이슈 title prefix**: 사용자가 `[SHJ]` 같은 prefix를 붙이는 경우가 있음 — 원본 이슈는 prefix 없음, 새 이슈만 prefix. 확정 없으면 질문
- **파일 ≤ 10 위반 위험 영역**: 한 이슈가 프로덕션 6~7 + 테스트 3~4 + Playwright 1 = 10~12를 가볍게 넘는 인증·크로스레이어 기능. Playwright 분리 여부로 파일 수가 자주 결정됨

## 레퍼런스 세션: 이슈 #8 → #15·#16 분할 예시

```
Phase 1: gh issue view 8  → 프로덕션 6 + 수정 2 + 테스트 3~4 감지
Phase 2: 3개 Explore 병렬 — 기존 auth/user 구조, 테스트 관용, SecurityConfig·yml
Phase 3: 예측 11~12 → CLAUDE.md 10 제약 위반 → AskUserQuestion 분할 3지선다
          사용자 선택: 표준 분할 + [SHJ] prefix
Phase 5: plan 파일 작성 — Context, 분할 결정, 이슈 A/B 파일 표, TDD RED, 재사용, 검증
Phase 6: 브랜치 suffix 추후 "-shj" 피드백 받아 plan Edit
Phase 7: ExitPlanMode → 승인
         ⇣ (plan-issue 범위 여기까지)
(실행): gh issue create × 2 → #15, #16 → feature/jwt-infra-shj → TDD → raise-pr 스킬
```

## 다른 스킬과의 관계
- **후행**: `raise-pr` — plan-issue 가 만든 계획·브랜치·커밋 이후의 PR 라이프사이클을 담당
- **보조**: `manual-test` — plan에 UI 시연·증적 필요가 포함되면 TDD 사이클 후·PR 전에 호출
- **범위 밖**: 코드 작성/커밋/머지 — plan-issue는 ExitPlanMode 까지만
