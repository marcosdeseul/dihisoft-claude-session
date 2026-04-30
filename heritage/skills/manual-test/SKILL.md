---
name: manual-test
description: Playwright MCP로 실제 브라우저(non-headless 선호)를 띄워 UI 플로우를 end-to-end로 시연하고, 각 상태마다 스크린샷을 남겨 수용 기준과 일치하는지 육안 검증한다. 사용자가 "실제 브라우저로 돌려봐", "UI 직접 확인", "수동 테스트", "manual test", "브라우저에서 보여줘" 등으로 특정 화면/플로우의 동작 확인을 요청할 때 호출한다. 자동화 테스트가 통과했더라도 실제 렌더링·스타일·상호작용을 눈으로 확인하는 용도.
---

# manual-test — Playwright MCP 기반 UI 수동 시연

프로젝트의 화면을 실제 브라우저로 열어 **시연하고 증적(스크린샷)을 남기는** 워크플로우.

## 언제 사용하는가
- PR 리뷰 전 화면 렌더/레이아웃 육안 확인
- 자동화 테스트는 통과했지만 CSS·메시지·UX 를 직접 확인하고 싶을 때
- 버그 재현 시나리오를 단계별 스크린샷으로 기록해야 할 때
- TDD 수용 기준의 "UI 렌더 계약" 체크박스를 증적 포함으로 완료할 때

사용하지 말 것: 회귀 방지용 자동 시나리오 (별도 e2e/통합 테스트로 고정).

## 전제
- 프로젝트의 dev 기동 명령(`$DEV_START_CMD`), 기동 완료 패턴(`$READY_PATTERN`), 기본 포트(`$DEV_PORT`) 가 정의되어 있다 (CLAUDE.md 어댑터 표 참조).
- 현재 세션에서 Playwright MCP 사용 가능 (`mcp__playwright__browser_*`).
- 언어 런타임 PATH 가 필요한 프로젝트면 `$RUNTIME_PATH` 도 정의되어 있다.

## 워크플로우

### 1. 대상 플로우 확인 (맥락에 없으면 짧게 질문)
- 어떤 경로 (예: `/signup`, `/login`, 특정 대시보드)
- 어떤 시나리오 (happy / 중복 / 검증 실패 / 권한 없음 등)
- 시연 후 서버 유지 여부

### 2. 서버 기동 (백그라운드)

dev 서버가 아직 떠 있지 않으면:

- **인프라 의존이 있거나(`$INFRA_CHECK_CMD`/`$ENV_BOOTSTRAP_CMD` 정의) 포트 폴백·헬스체크가 필요한 프로젝트** → `dev-server` 스킬 위임.
- **기동이 단순한 프로젝트** → 이 스킬 내부에서 직접:

```bash
# 필요한 경우 런타임 PATH 먼저
$RUNTIME_PATH

$DEV_START_CMD
```

- `run_in_background: true` 로 실행해 출력 파일 경로 확보.
- 이미 동일 포트에서 떠 있다면 재기동 대신 그대로 사용 (포트 점유 확인: `lsof -i :$DEV_PORT`).

### 3. 기동 대기

`Monitor` 로 출력 파일을 tail + grep (line-buffered):

```bash
tail -f <output-file> | grep -E --line-buffered \
  "$READY_PATTERN|APPLICATION FAILED|BUILD FAILED|Port .* (already|in use)|EADDRINUSE" | head -1
```

- 성공: `$READY_PATTERN` 매칭
- 실패: 그 외 패턴 — 즉시 중단하고 사용자에게 로그 일부와 함께 보고

### 4. 브라우저 시연 — 각 시나리오 3단계 증적

Playwright MCP 툴 사용 패턴:

1. **초기 상태**
   - `browser_navigate` → URL 이동
   - `browser_snapshot` → 접근성 트리 확인 (엘리먼트 `ref=eN` 확보; fill/click 에 필수)
   - `browser_take_screenshot` → `<feature>-empty.png` 또는 `-initial.png`

2. **입력/상호작용**
   - `browser_fill_form` (여러 필드 일괄) 또는 `browser_type` (단일 필드)
   - 선택: `browser_take_screenshot` → `<feature>-filled.png` (바인딩 확인용)

3. **제출 결과**
   - `browser_click` 으로 submit
   - 페이지 전환 시 새 snapshot 이 tool result 에 포함됨
   - `browser_take_screenshot` → `<feature>-success.png` 또는 `-<case>-error.png`
   - URL 전환, 성공 배너, 에러 메시지 육안 확인

시나리오를 여러 개 돌릴 때는 사이에 `browser_navigate` 로 초기 URL 재진입 (폼/상태 리셋).

### 5. 정리
- `mcp__playwright__browser_close` — 페이지 닫기
- `TaskStop` — 백그라운드 dev 서버 태스크 중단 (사용자가 "켜둬" 라고 하지 않은 한)
- `Monitor` 도 정리

### 6. 결과 보고 (표 형태)

| 스크린샷 | 시나리오 | 결과 |
|---|---|---|
| `<name>-empty.png` | 초기 로드 | 폼 렌더, 빈 상태 |
| `<name>-success.png` | 정상 입력 | 리다이렉트 + 성공 메시지 |
| `<name>-<case>-error.png` | 오류 케이스 | 필드별/글로벌 오류 렌더 |

자동 테스트가 검증하는 계약과 실제 렌더링이 일치하는지 명시. 불일치 발견 시 즉시 보고.

## 가이드라인

- **비헤드리스 기본**: 사용자가 "실제로 브라우저 띄워" 요청 시 Playwright MCP 기본 모드 그대로. 자동화 테스트는 별도(헤드리스). 이 스킬은 시연·증적용.
- **엘리먼트 참조는 snapshot 에서만**: `browser_snapshot` 이 반환한 `ref=eN` 값을 `fill_form`/`click` 의 `ref` 에 그대로 전달. CSS selector 추측 금지.
- **스크린샷 네이밍**: `<feature>-<state>.png`. 예: `signup-empty.png`, `signup-success.png`, `signup-duplicate-error.png`, `signup-validation-error.png`.
- **기동 시간 대응**: 첫 기동 시간이 있을 수 있다. `Monitor` 시그널 대기 후 navigate. `sleep` 조합 금지.
- **한 시나리오는 한 턴에 몰아서**: navigate → snapshot → fill → click → screenshot. 중간 설명 최소화.
- **자격증명**: 자격증명이 필요한 경로면 프로젝트의 E2E 자격증명 환경변수(`$E2E_CRED_ENV`) 또는 사용자에게 즉석 회원가입을 요청. 자격증명을 본 파일에 박지 마라.
- **실패·오차 발견 시 즉시 보고**: 자동 테스트가 예상한 문자열과 실제 화면이 다르면, 테스트 계약과 UI 문구 중 어느 쪽을 맞출지 사용자에게 결정 요청.

## 후속 제안

시연 중 자동화하면 좋은 케이스를 발견하면 다음 중 하나로 고정:
- 통합 테스트 추가 (빠르고 CI 기본 포함)
- 브라우저 e2e 테스트 추가 (실제 브라우저, 옵트인 또는 nightly)

## 다른 스킬과의 관계

- **호출자**: `create-issue` (재현), `plan-issue` (재현), `raise-pr` (UI 변경 PR 의 증적), `local-tests` (UI 변경 감지 시 보조)
- **호출**: `dev-server` (인프라 의존이 있거나 포트 폴백·헬스체크가 필요할 때)
- **범위 밖**: 자동 회귀 테스트 작성·실행, 서버 기동 자체의 디버깅 (별도)
