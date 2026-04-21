---
name: manual-test
description: Playwright MCP로 실제 브라우저(non-headless 선호)를 띄워 이 프로젝트의 UI 플로우를 end-to-end로 시연하고, 각 상태마다 스크린샷을 남겨 수용 기준과 일치하는지 육안 검증한다. 사용자가 "실제 브라우저로 돌려봐", "UI 직접 확인", "수동 테스트", "manual test", "브라우저에서 보여줘" 등으로 특정 화면/플로우의 동작 확인을 요청할 때 호출한다. 자동화 테스트(MockMvc/`@Tag("playwright")`)가 통과했더라도 실제 렌더링·스타일·상호작용을 눈으로 확인하는 용도.
---

# manual-test — Playwright MCP 기반 UI 수동 시연

이 프로젝트(Spring Boot + Thymeleaf) 화면을 실제 브라우저로 열어 **시연하고 증적(스크린샷)을 남기는** 워크플로우.

## 언제 사용하는가
- PR 리뷰 전 화면 렌더/레이아웃 육안 확인
- 자동화 테스트는 통과했지만 CSS·메시지·UX를 직접 확인하고 싶을 때
- 버그 재현 시나리오를 단계별 스크린샷으로 기록해야 할 때
- TDD 수용 기준의 "UI 렌더 계약" 체크박스를 증적 포함으로 완료할 때

사용하지 말 것: 회귀 방지용 자동 시나리오(MockMvc/`@Tag("playwright")` 테스트에 고정).

## 전제
- 프로젝트 루트에 `gradlew` 존재 (Spring Boot 앱)
- 현재 세션에서 Playwright MCP 사용 가능 (`mcp__playwright__browser_*`)
- Java 21 실행 가능 (`/opt/homebrew/opt/openjdk@21/bin` PATH)

## 워크플로우

### 1. 대상 플로우 확인 (맥락에 없으면 짧게 질문)
- 어떤 경로 (예: `/signup`, `/login`, `/api/posts/{id}`)
- 어떤 시나리오 (happy / 중복 / 검증 실패 / 권한 없음 등)
- 시연 후 서버 유지 여부

### 2. 서버 기동 (백그라운드)
```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH" && ./gradlew bootRun
```
- `run_in_background: true` 로 실행해 출력 파일 경로 확보
- 기본 포트 `8080`

### 3. 기동 대기
`Monitor`로 출력 파일을 tail + grep (line-buffered):
```
tail -f <output-file> | grep -E --line-buffered \
  "Started BoardApplication|APPLICATION FAILED|BUILD FAILED|Port .* already" | head -1
```
- 성공: `Started BoardApplication`
- 실패: `APPLICATION FAILED` / `BUILD FAILED` / `Port .* already`

### 4. 브라우저 시연 — 각 시나리오 3단계 증적
Playwright MCP 툴 사용 패턴:

1. **초기 상태**
   - `browser_navigate` → URL 이동
   - `browser_snapshot` → 접근성 트리 확인 (엘리먼트 `ref=eN` 확보; fill/click에 필수)
   - `browser_take_screenshot` → `<feature>-empty.png` 또는 `-initial.png`

2. **입력/상호작용**
   - `browser_fill_form` (여러 필드 일괄) 또는 `browser_type` (단일 필드)
   - 선택: `browser_take_screenshot` → `<feature>-filled.png` (바인딩 확인용)

3. **제출 결과**
   - `browser_click` 로 submit
   - 페이지 전환 시 새 snapshot이 tool result에 포함됨
   - `browser_take_screenshot` → `<feature>-success.png` 또는 `-<case>-error.png`
   - URL 전환, 성공 배너, 에러 메시지 육안 확인

시나리오를 여러 개 돌릴 때는 사이에 `browser_navigate`로 초기 URL 재진입 (폼/상태 리셋).

### 5. 정리
- `mcp__playwright__browser_close` — 페이지 닫기
- `TaskStop` — bootRun 백그라운드 태스크 중단 (사용자가 "켜둬"라고 하지 않은 한)
- `Monitor`도 정리 (자동 타임아웃 무시 가능)

### 6. 결과 보고 (표 형태)
| 스크린샷 | 시나리오 | 결과 |
|---|---|---|
| `<name>-empty.png` | 초기 로드 | 폼 렌더, 빈 상태 |
| `<name>-success.png` | 정상 입력 | 302 리다이렉트, 성공 메시지 |
| `<name>-<case>-error.png` | 오류 케이스 | 필드별/글로벌 오류 렌더 |

자동 테스트가 검증하는 계약과 실제 렌더링이 일치하는지 명시. 불일치 발견 시 즉시 보고.

## 가이드라인

- **비헤드리스 기본**: 사용자가 "실제로 브라우저 띄워" 요청 시 Playwright MCP 기본 모드 그대로. 기존 `SignupPlaywrightTest` 등 자동 테스트는 헤드리스 유지 (이 스킬은 시연·증적용).
- **엘리먼트 참조는 snapshot에서만**: `browser_snapshot` 반환한 `ref=eN` 값을 `fill_form`/`click`의 `ref`에 그대로 전달. CSS selector 추측 금지.
- **스크린샷 네이밍**: `<feature>-<state>.png`. 예: `signup-empty.png`, `signup-success.png`, `signup-duplicate-error.png`, `signup-validation-error.png`.
- **기동 시간 대응**: 첫 기동 1.5~3초. `Monitor` 시그널 대기 후 navigate. `sleep` 조합 금지.
- **한 시나리오는 한 턴에 몰아서**: navigate → snapshot → fill → click → screenshot. 중간 설명 최소화.
- **실패·오차 발견 시 즉시 보고**: 자동 테스트가 예상한 문자열("가입 완료" 등)과 실제 화면이 다르면, 테스트 계약과 UI 문구 중 어느 쪽을 맞출지 사용자에게 결정 요청.

## 이 프로젝트에서 자주 쓰는 경로

- `GET /signup` — Thymeleaf 회원가입 폼 (2.1, #7 머지 이후)
- `GET /login` — 로그인 폼 (2.2, #8 예정)
- `GET /h2-console` — H2 관리 콘솔 (공개)
- `GET /api/...` — REST (대부분 인증 필요 → 2.2 이후 JWT 발급 후 시연 가능)

UI 없는 REST-only 엔드포인트는 `curl` 스크립트를 권장 (이 스킬 범위 밖).

## 후속 제안

시연 중 자동화하면 좋은 케이스를 발견하면 다음 중 하나로 고정:
- MockMvc 통합 테스트 추가 (빠르고 CI 기본 포함)
- `@Tag("playwright")` 테스트 추가 (실제 브라우저, 로컬 `-PincludePlaywright`로 실행)

---

## 실행 예시 — 2.1 회원가입 시연 실제 기록

```
1) ./gradlew bootRun (background) → Monitor("Started BoardApplication") 대기
2) browser_navigate /signup → browser_snapshot → screenshot empty
3) browser_fill_form({username:"demo_user", password:"pw12345demo"}) → browser_click 가입
   → screenshot success (302 /signup?success=true, "가입 완료! 이어서 로그인 페이지에서 로그인하세요.")
4) browser_navigate /signup → fill same username → click
   → screenshot duplicate-error ("이미 사용 중인 username입니다: demo_user")
5) browser_navigate /signup → fill "ab"/"12345" → click
   → screenshot validation-error (필드별: "username은 3~20자여야 합니다" / "password는 최소 6자여야 합니다")
6) browser_close + TaskStop bootRun
7) 표 형태 요약 보고
```
