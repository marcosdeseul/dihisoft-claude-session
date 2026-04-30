---
name: raise-pr
description: 이미 구현·커밋이 완료된 로컬 브랜치를 기준으로 PR을 생성하고, CI를 통과시켜 리뷰 승인 대기까지 끌고 가는 워크플로우. 사용자가 "PR 올려줘", "raise pr", "PR 생성", "이 브랜치로 PR", "리뷰 요청" 등으로 요청할 때 호출한다. 이슈 분석·브랜치 생성·TDD 구현은 범위 밖(`plan-issue` 또는 자유 구현 흐름). 이 스킬은 push → PR 본문 작성 → CI 감시 → 실패 대응 → 이슈·PR 체크박스 갱신 → 머지 대기까지를 일관되게 수행한다.
---

# raise-pr — 완료된 브랜치에서 PR 을 올려 승인까지

CLAUDE.md 의 작업 단위 제약(≤10 파일, <300줄)과 커버리지 게이트(`$COVERAGE_THRESHOLD`)를 준수하며, **구현이 끝난 로컬 브랜치를 받아 PR 라이프사이클만** 일관 처리한다.

## 전제
- 현재 로컬 브랜치에 필요한 커밋이 모두 있고, **미커밋 변경 없음**.
- `$DEFAULT_BRANCH` 에 비해 의미 있는 diff 가 있음 (`git log --oneline $DEFAULT_BRANCH..HEAD` 비어 있지 않음).
- `gh` CLI 인증 완료, 원격 `origin` 설정됨.
- 어댑터 변수 정의됨: `$LOCAL_GATE_CMD`, `$COVERAGE_THRESHOLD`, `$CI_TEST_JOB`, `$CI_BUILD_JOB`, `$DEFAULT_BRANCH`, (필요 시) `$RUNTIME_PATH`.

아직 코드가 다 안 되어 있다면 이 스킬이 아니라 `plan-issue` 또는 직접 구현 흐름을 먼저 마쳐야 한다.

## 언제 사용하는가
- 브랜치 작업이 끝났고 PR 로 올릴 차례
- 올린 PR 의 CI 실패 후 수정 push → 재검증 루프
- PR 과 이슈 체크박스 정합성 감사·갱신

## Phase 1 — 사전 점검 (Push 전)

### 1.1 현재 상태
```bash
git branch --show-current
git status                                 # 미커밋 없음 확인
git log --oneline $DEFAULT_BRANCH..HEAD    # 커밋 목록
git diff --stat $DEFAULT_BRANCH...HEAD     # 파일 변경 요약
```

### 1.2 CLAUDE.md 규칙 점검
- **수정 파일 ≤ 10**: `git diff --name-only $DEFAULT_BRANCH...HEAD | wc -l` 확인 (빌드 도구 자동 생성 파일 제외).
  - 초과 시 **PR 올리지 말고** 사용자에게 `x.1/x.2` 로 분할 제안. 예외는 foundation 류처럼 사용자 사전 합의가 있을 때만.
- **파일당 <300줄**: `wc -l` 로 신규 파일 라인 수 체크. 초과면 책임 분리/테스트 클래스 분할 권고.

### 1.3 로컬 품질 게이트
```bash
$RUNTIME_PATH    # 필요한 경우만
$LOCAL_GATE_CMD
```
- `$LOCAL_GATE_CMD` 는 **lint + test + 커버리지 게이트(`$COVERAGE_THRESHOLD`) + 빌드** 를 한 방에 도는 명령으로 정의되어야 한다.
- UI 포함 변경이면 **`manual-test` 스킬** 로 시연·스크린샷 1~2장 확보.

## Phase 2 — Push + PR 생성

### 2.1 Push
```bash
git push -u origin <current-branch>
```
기존 PR 이 있으면 자동으로 업데이트되고 CI 재실행. 없으면 신규 upstream.

### 2.2 PR 본문 작성 (heredoc → `/tmp/pr-body.md`)

**표준 템플릿**
```markdown
Closes #<issue-num>

## Summary
- 1~3개 불릿: 무엇을/왜 (변경의 핵심)

## TDD 증적 (해당 시)
- `test: ...` → `feat: ...` 커밋 쌍
- RED 에서 선제 설계한 edge case (입력검증/권한/경계값/리소스상태/형식)

## Test plan
- [x] `$LOCAL_GATE_CMD` 로컬 초록불 (N tests PASS, 커버리지 X.X%)
- [ ] CI 초록불 (PR 생성 직후 확인) — `$CI_TEST_JOB` + `$CI_BUILD_JOB` 두 잡
- [ ] (UI 포함 시) 로컬 dev 수동 확인 + manual-test 스크린샷

## 파일 변경 요약
| # | 파일 | 구분 | 비고 |
|---|---|---|---|
| 1 | `경로/...` | 신규/수정 | ... |

## 주의 (필요 시)
- ≤10 파일 규칙 예외 사유 / 선행 이슈 전제 / 범위 밖 명시

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### 2.3 PR 생성
```bash
gh pr create \
  --title "<type>(<area>): #<num> <요약 ≤70자>" \
  --body-file /tmp/pr-body.md
```
- 제목 컨벤션: `feat(auth): #7 ...`, `ci: ...`, `docs: ...`, `chore: ...`, `test: ...`, `refactor: ...`, `fix: ...`.
- 여러 이슈를 닫으려면 본문에 `Closes #A`, `Closes #B` 각각 줄 바꿈.
- 반환된 PR URL·번호를 기록.

## Phase 3 — CI 대기·실패 대응 루프

### 3.1 감시
```bash
gh pr checks <pr-num>
```
또는 `Monitor` 도구로 `until` 루프:
```bash
until out=$(gh pr checks <pr-num> 2>&1); \
  echo "$out" | grep -qE "($CI_TEST_JOB|$CI_BUILD_JOB).*\s(pass|fail|cancelled)"; \
  do sleep 8; done; echo "$out"
```

프로젝트의 두 잡:
- `$CI_TEST_JOB` — test + 커버리지 + (있다면) Codecov 업로드 + **`$COVERAGE_THRESHOLD` 검증**
- `$CI_BUILD_JOB` — 빌드 (테스트 제외)

### 3.2 실패 시 원인 격리
```bash
gh run view <run-id> --log-failed \
  | grep -E "FAILED|Rule violated|AssertionError|Error:|BUILD FAILED" \
  | head -20
```

**원인별 대응**
- **커버리지 미달 (`$COVERAGE_THRESHOLD` 위반)**:
  - 보통 가장 큰 단일 공백 클래스/모듈 하나를 유닛 테스트로 직접 exercise 하면 가성비 최고
  - 테스트 파일 신규 추가 시에도 **≤10 파일·<300줄** 계속 준수
- **테스트 실패**: 빌드 도구의 리포트(예: `build/reports/tests/...` 또는 CI 아티팩트) 로 스택 확인 → 원인 수정
- **빌드 오류**: 의존성/설정 누락 확인

### 3.3 수정 커밋 → 재검증
- 메시지 컨벤션: `test: 게이트 달성을 위한 X 유닛 테스트 추가` / `fix: ...`
- `git push` → CI 자동 재실행 → Monitor 재가동 (새 PR 번호 아님, 동일 PR 업데이트)

## Phase 4 — 이슈 ↔ PR 적합성 감사

### 4.1 체크박스 1:1 대조
```bash
gh issue view <num> --json body --jq '.body' > /tmp/issue-body-old.md
```
각 `- [ ]` 항목을 PR 구현 범위와 대조. 구현 완료 + 증적(테스트명/파일 경로) 확보한 것만 `[x]` 전환.

### 4.2 이슈 본문 갱신
```bash
# /tmp/issue-body-new.md 편집 후
gh issue edit <num> --body-file /tmp/issue-body-new.md
```
- 미완 항목은 **사실대로 `[ ]` 유지** + 짧은 각주 (\*이후 이슈에서\*, \*로컬 검증 미완\* 등).
- 허위 체크 금지.

### 4.3 PR Test plan 갱신
```bash
gh pr view <pr-num> --json body --jq '.body' > /tmp/pr-body-old.md
# 체크박스 [x] 전환 + CI run 링크·측정치 삽입 후
gh pr edit <pr-num> --body-file /tmp/pr-body-new.md
```

## Phase 5 — 리뷰·승인·머지 대기

### 5.1 머지는 사용자가 한다
**Claude 는 `gh pr merge` 자동 실행 금지.** 공유 상태 변경은 사용자 명시적 승인 필요.
리뷰 코멘트 있으면 해당 사항만 수정해 Phase 3 부터 재진행.

### 5.2 머지 후 정리
```bash
git fetch origin --prune                               # 삭제된 원격 브랜치 반영
git checkout $DEFAULT_BRANCH && git pull --ff-only
```
상위 에픽 이슈가 있다면 해당 서브 이슈 체크박스 `[x]` 갱신.

---

## 가이드라인 (박스 요약)

- **Push 전 필수 3가지**: 미커밋 없음 / 파일 ≤10 / `$LOCAL_GATE_CMD` 초록불 (커버리지 게이트 포함)
- **금지 행동 (사용자 승인 없이 금지)**: `gh pr merge`, `git push --force-with-lease`/`--force`, `git reset --hard`, `gh run cancel`
- **PR 본문 템플릿**: `Closes #N` → Summary → TDD 증적 → Test plan → 파일 표 → 주의 → Claude 푸터
- **CI pending 1~2분 정상**: `Monitor` 의 `until` 루프로 감시. `sleep` 체인 금지.
- **커버리지 실패 시 우선순위**: 단일 최대 공백 클래스/모듈을 유닛 테스트로 직접 exercise
- **허위 체크박스 금지**: 이슈·PR 본문 체크는 실제 구현·검증 기반으로만

## 다른 스킬과의 관계

- **선행**: `plan-issue` — 이슈 분석·질문·플랜 수립. 이 스킬은 그 결과물을 전제로 시작.
- **호출**: `manual-test` — UI 포함 변경이면 Phase 1 의 로컬 게이트나 Phase 4 의 증적 확보 시 사용. `local-tests` — Phase 1.3 게이트로 `$LOCAL_GATE_CMD` 대신 정밀 검증을 원할 때.
- **후속**: `multi-agent-review` — PR 이 올라간 뒤 머지 전 다관점 리뷰.
- **범위 밖**: 자체 코드 리뷰, 머지 자동 실행, 다른 저장소·시스템 알림.
