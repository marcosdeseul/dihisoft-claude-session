---
name: local-tests
description: 커밋·push 전에 프로젝트의 모든 로컬 검증(lint, 타입 체크, 단위 테스트, 커버리지, 시각/E2E)을 한 번에 돌려 결과를 요약한다. 사용자가 "로컬 테스트 돌려줘", "전체 검사", "lint+test 같이 돌려", "run all tests", "local tests" 등으로 요청할 때 호출한다. 변경된 파일을 감지해 시각/E2E 검증은 필요할 때만 실행한다. 단일 명령(예: `$LOCAL_GATE_CMD`) 한 방으로 끝나는 프로젝트에선 `raise-pr` 의 Phase 1.3 으로 충분하지만, 다중 빌드 도구(예: 백엔드 + 프런트엔드 + Storybook) 가 있는 프로젝트에서 단계별 가시성이 필요할 때 이 스킬을 사용한다.
---

# local-tests — 커밋·push 전 전체 로컬 검증

`$LINT_CMD`, `$TYPECHECK_CMD`, `$TEST_CMD`, `$COVERAGE_CMD`, (선택) `$STORYBOOK_CMD`, (선택) E2E 를 순차 실행하며, 단계별 결과·실패 격리·재실행 안내를 제공한다.

## 트리거
- "로컬 테스트 돌려줘", "run tests", "run all tests", "run local tests"
- "lint + test 같이", "전체 검사", "verify code"
- 커밋/push 전 마지막 검증

## 전제
- 어댑터 변수가 정의되어 있다 (CLAUDE.md 의 어댑터 표 참조). 정의되지 않은 변수의 단계는 **자동 스킵**하고 사용자에게 정의를 요청한다.

## 워크플로우

### Step 0: 작업 디렉토리 확인
```bash
pwd
git rev-parse --show-toplevel    # 저장소 루트
```
명령 실행 디렉토리(루트 vs 서브 모듈)를 확인. 일부 프로젝트는 `frontend/` 등 서브 디렉토리에서 npm 명령을 실행해야 한다.

### Step 1: 변경 파일 감지

```bash
# 패치 비교 기준은 PR 분기점
git diff --name-only origin/$DEFAULT_BRANCH...HEAD
# 또는 작업 중이면
git diff --name-only HEAD
```

이 목록을 다음 단계의 **시각/E2E 자동 트리거** 판단과 **커버리지 검증 범위** 산정에 사용한다.

### Step 2: 진행 상황 트래킹

`TaskCreate` 로 단계별 체크리스트:
1. Lint
2. Typecheck (해당 시)
3. Unit tests
4. Coverage 검증 (`$COVERAGE_THRESHOLD`)
5. 시각 컴포넌트 테스트 (UI 변경 시)
6. E2E 테스트 (정의되어 있고 변경이 관련될 때)

### Step 3: Lint
```bash
$LINT_CMD 2>&1 | tail -30
```
**실패 시**: 즉시 중단, 에러 보고. 다음 단계로 가지 않는다.

### Step 4: Typecheck (해당 시)
```bash
$TYPECHECK_CMD 2>&1 | tail -30
```
타입 시스템이 없는 언어이거나 컴파일이 곧 타입 체크인 경우 스킵. 변수가 정의되지 않았으면 자동 스킵.

**실패 시**: 즉시 중단.

### Step 5: 단위 테스트 + 커버리지
```bash
$COVERAGE_CMD 2>&1 | tail -100
```
보통 `$COVERAGE_CMD` 는 단위 테스트 실행을 포함한다. 분리되어 있다면 `$TEST_CMD` 후 별도 커버리지 실행.

**실패 시**: 실패 테스트 이름과 함께 보고. 중단.

### Step 6: 변경 파일 커버리지 점검 (`$COVERAGE_THRESHOLD`)

**범위**: 전체 코드베이스가 아니라 **이 PR 에서 변경된 파일**.

Step 1 의 변경 파일 목록 중 소스 파일(`$SOURCE_EXTS` 매칭) 만 골라 `$COVERAGE_CMD` 결과 표에서 그 파일들의 라인을 확인.

**판정 기준** (`$COVERAGE_THRESHOLD` 표현 예시):
- "Jest patch 95%" → 변경된 `.ts/.tsx` 파일이 95% 이상
- "JaCoCo INSTRUCTION 95%" → 모듈 전체 95% 이상 (게이트가 클래스 단위가 아닌 경우 게이트 자체가 PASS 면 충분)
- "coverage.py 90%" → 패치 80% 이상

**미달 시**: 어떤 파일·어떤 라인이 누락되었는지 보고. 사용자에게 (1) 추가 테스트 작성, (2) 일시 게이트 완화 중 선택 요청. 게이트 완화는 강하게 비권장 (CLAUDE.md ZERO TOLERANCE).

### Step 7: 시각 컴포넌트 테스트 (옵션, 자동 감지)

**조건**: 모두 만족할 때만 실행.
- `$STORYBOOK_CMD` 가 정의되어 있다
- Step 1 변경 파일에 UI 확장자(`$SOURCE_EXTS` 중 UI 부분)가 포함된다
- 프로젝트에 stories 가 존재한다 (예: `**/*.stories.{ts,tsx,js,jsx}`)

```bash
$STORYBOOK_CMD 2>&1 | tail -100
```

**흔한 실패**:
- 접근성(a11y) 위반 → 컴포넌트 라벨/대비 수정
- 인터랙션 테스트 실패 → 컴포넌트 동작 또는 스토리 mock 갱신
- 타임아웃 → 데이터 로드 mock 확인

### Step 8: E2E (옵션, 자동 감지)

**조건**: 모두 만족할 때만 실행.
- 프로젝트에 e2e 디렉토리(또는 동등) 존재
- 사용자가 명시적으로 요청 OR 변경이 라우팅/세션/외부 통합에 닿음

자격증명이 필요하면 `$E2E_CRED_ENV` 환경변수에서 읽는다. **자격증명을 본 스킬·코드에 박지 마라.** 변수가 비어 있으면 사용자에게 즉석 발급/투입 요청.

### Step 9: 결과 요약

```
✅ Lint: passed
✅ Typecheck: passed (또는 skipped)
✅ Unit tests: N passed
✅ Coverage: 변경 파일 X.X% (게이트 통과)
✅ Storybook: M stories passed (또는 skipped — 사유)
✅ E2E: K tests passed (또는 skipped — 사유)
```

실패가 있다면 실패한 단계만 강조해 보고하고, 다음 행동을 1~3개 제안.

## 컨텍스트 인지 규칙

**스킵 조건**:
- 같은 대화 중 이미 통과한 단계
- 사용자가 "그건 이미 통과했다" 라고 명시

**무조건 실행**:
- 이전 실행에서 실패한 단계 (수정 후 재검증)
- 사용자가 명시적으로 재실행 요청
- 어댑터 변수가 변경됨

## CRITICAL: ZERO TOLERANCE

**테스트 실패의 소유권은 작업자에게 있다.** (CLAUDE.md ZERO TOLERANCE 섹션과 동일)

금지 표현:
- ❌ "내 변경과 무관한 실패"
- ❌ "원래 깨져 있었다"
- ❌ "플레이키 테스트"
- ❌ "이번엔 스킵"

필수 행동:
- ✅ 실패 원인 추적
- ✅ 근본 원인 수정 (테스트가 아니라 코드)
- ✅ 모두 초록불이 될 때까지 "완료" 로 표시하지 않음

## 다른 스킬과의 관계

- **호출자**: `create-issue`, `plan-issue`, `raise-pr` (모두 Step 8/Phase 1.3 등에서 호출)
- **호출**: `manual-test` (Step 7 시각 테스트가 부족할 때 보조 수동 검증)
- **범위 밖**: PR 생성·푸시 (`raise-pr`), 커버리지 미달 시 자동 테스트 작성 (사용자 승인 필요)
