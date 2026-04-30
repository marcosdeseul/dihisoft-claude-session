---
name: manual-test
description: 변경 분석에 기반해 (UI 변경이면) Playwright MCP 로 실제 브라우저를 띄우거나 (API/MCP 변경이면) `curl` 로 엔드포인트를 호출해 수동 시연·증적을 남긴다. 모든 테스트 실행 전에 **edge case 까지 포함한 시나리오를 먼저 제안**하고 **사용자 승인을 받은 뒤에만** 실제 호출을 시작한다. 사용자가 "수동 테스트", "manual test", "UI 직접 확인", "API 한번 찔러봐", "브라우저에서 보여줘", "실제로 돌려봐" 등으로 요청할 때 호출한다. 자동화 테스트가 통과했더라도 실제 렌더링·스타일·상호작용이나 API 응답 형태를 눈으로 확인하는 용도.
---

# manual-test — 변경 타입 자동 판별 + 시나리오 승인 후 수동 시연

UI 변경이면 Playwright MCP, API/MCP 변경이면 `curl` — 변경 분석으로 모드를 결정하고, **edge case 까지 포함한 시나리오 제안 → 사용자 승인 → 실행** 의 순서를 항상 지킨다.

## 언제 사용하는가
- PR 리뷰 전 화면 렌더/레이아웃·API 응답 형태 육안 확인
- 자동화 테스트는 통과했지만 실제 동작(CSS·메시지·UX·응답 바디·상태 코드)을 직접 확인하고 싶을 때
- 버그 재현 시나리오를 단계별 증적으로 기록해야 할 때

사용하지 말 것: 회귀 방지용 자동 시나리오 (별도 e2e/통합 테스트로 고정).

## 전제
- 프로젝트의 dev 기동 명령(`$DEV_START_CMD`), 기동 완료 패턴(`$READY_PATTERN`), 기본 포트(`$DEV_PORT`) 가 정의되어 있다 (CLAUDE.md 어댑터 표 참조).
- UI 시나리오: 현재 세션에서 Playwright MCP 사용 가능 (`mcp__playwright__browser_*`).
- API/MCP 시나리오: `curl` 사용 가능. 인증이 필요한 엔드포인트면 토큰/키 발급 경로(예: 로그인 폼, Settings 화면, env 변수) 가 식별 가능.
- 언어 런타임 PATH 가 필요한 프로젝트면 `$RUNTIME_PATH` 도 정의되어 있다.

## CRITICAL Rule

**시나리오 제안 + 사용자 승인 게이트(Step 2) 통과 전에는 어떤 실제 호출도 하지 않는다.**
- 브라우저 navigate / `browser_*` 호출 금지
- `curl` 호출 금지
- dev 서버 기동조차 사용자 승인 후에 시작 (재현용 서버가 이미 떠 있는 경우는 예외)

이 게이트가 무너지면 토큰/세션 낭비, 의도와 다른 시나리오 실행, 사용자 검증 누락으로 이어진다.

## 워크플로우

### Step 1: 변경 분석 → 모드 판별

먼저 무엇이 바뀌었는지 본다. 사용자가 명시했다면 그대로 따르되, 명시가 없으면 git diff 로 자동 판별:

```bash
# 변경 파일 목록 (어댑터의 $SOURCE_EXTS 가 정의되어 있으면 활용)
git diff --name-only origin/$DEFAULT_BRANCH...HEAD
```

판별 규칙:

| 변경 패턴 | 모드 |
|---|---|
| 템플릿/뷰/CSS/JS·TSX 컴포넌트 (사용자가 보는 화면) | **UI 모드** (Playwright MCP) |
| 컨트롤러·라우트·DTO·서비스만 변경, 뷰 무수정 | **API 모드** (curl) |
| MCP tool 정의·핸들러만 변경 | **API 모드** (curl, MCP JSON-RPC) |
| 양쪽 다 변경 | **두 모드 모두 제안** (UI → API 순서로 시나리오 분리) |
| 변경 파일이 없거나 사용자가 명시한 시나리오만 있음 | 사용자 의도 따라 |

판별 결과를 사용자에게 한 줄로 알린다 — 잘못 판단했다면 사용자가 즉시 정정할 수 있다.

### Step 2: 시나리오 제안 + 사용자 승인 게이트 (MANDATORY)

실행 전에 **edge case 까지 포함한 시나리오 표** 를 제안하고, `AskUserQuestion` 으로 명시 승인을 받는다.

#### 2.1 시나리오 표 작성

각 시나리오는 다음을 포함:
- **이름** (예: `signup-happy`, `signup-duplicate`, `posts-list-pagination`)
- **타입** (UI / API)
- **대상** (URL · 메소드 + 경로)
- **입력**
- **기대 결과** (상태 코드, 응답 필드, 화면 요소, 에러 메시지)
- **edge case 분류** (CLAUDE.md 체크리스트 적용 — 입력검증/인증/리소스상태/경계값/형식/부수효과/동시성)

happy path 만으로 끝내지 마라. **CLAUDE.md edge case 체크리스트** 의 항목 중 이 변경에 해당하는 것을 의식적으로 표에 채운다.

**UI 모드 표 예시**:

| # | 이름 | 대상 | 입력 | 기대 결과 | edge case |
|---|---|---|---|---|---|
| 1 | signup-empty | `GET /signup` | (없음) | 폼 렌더, 빈 상태 | (초기 로드) |
| 2 | signup-happy | `POST /signup` | username=`demo_user`, password=`pw12345` | 302 리다이렉트, 성공 메시지 | happy |
| 3 | signup-duplicate | `POST /signup` | 동일 username 재시도 | 동일 페이지 + "이미 사용 중" | 리소스 상태 (중복) |
| 4 | signup-validation | `POST /signup` | username=`ab`, password=`12345` | 필드별 검증 메시지 | 입력 검증 (경계값 미달) |

**API 모드 표 예시**:

| # | 이름 | 대상 | 입력 | 기대 결과 | edge case |
|---|---|---|---|---|---|
| 1 | posts-list | `GET /api/posts?page=0&size=10` | (인증 없음, 공개) | 200, `{content:[...], page:{...}}` | happy |
| 2 | posts-create-unauth | `POST /api/posts` | 토큰 없음 | 401 | 인증 |
| 3 | posts-create-happy | `POST /api/posts` | 정상 토큰 + 본문 | 201 + Location 헤더 | happy |
| 4 | posts-update-other-author | `PUT /api/posts/{id}` | 다른 사용자 토큰 | 403 | 권한 |
| 5 | posts-get-missing | `GET /api/posts/9999` | (없음) | 404 | 리소스 상태 (없음) |
| 6 | posts-create-empty-title | `POST /api/posts` | title="" | 400, 검증 메시지 | 입력 검증 (빈값) |

#### 2.2 사용자 승인 받기

`AskUserQuestion` 으로:
- 질문: "이 시나리오로 진행할까요?"
- 옵션: "진행" / "수정 필요" / "일부만 실행" / "edge case 추가"

**사용자가 명시 승인하기 전에는 Step 3 으로 가지 않는다.** "수정 필요" 또는 "edge case 추가" 면 표를 갱신하고 다시 승인 요청.

### Step 3: 서버 기동 (필요 시, 백그라운드)

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

### Step 4: 기동 대기

`Monitor` 로 출력 파일을 tail + grep (line-buffered):

```bash
tail -f <output-file> | grep -E --line-buffered \
  "$READY_PATTERN|APPLICATION FAILED|BUILD FAILED|Port .* (already|in use)|EADDRINUSE" | head -1
```

- 성공: `$READY_PATTERN` 매칭
- 실패: 그 외 패턴 → 즉시 중단, 로그 일부와 함께 사용자에게 보고

### Step 5a: UI 시연 (Playwright MCP — UI 모드)

각 시나리오마다 3단계 증적:

1. **초기 상태**
   - `browser_navigate` → URL 이동
   - `browser_snapshot` → 접근성 트리 확인 (엘리먼트 `ref=eN` 확보; fill/click 에 필수)
   - `browser_take_screenshot` → `<feature>-empty.png` 또는 `-initial.png`

2. **입력/상호작용**
   - `browser_fill_form` (여러 필드 일괄) 또는 `browser_type` (단일 필드)
   - 선택: `browser_take_screenshot` → `<feature>-filled.png`

3. **제출 결과**
   - `browser_click` 으로 submit
   - 페이지 전환 시 새 snapshot 이 tool result 에 포함됨
   - `browser_take_screenshot` → `<feature>-success.png` 또는 `-<case>-error.png`
   - URL 전환, 성공 배너, 에러 메시지 육안 확인

시나리오 사이에 `browser_navigate` 로 초기 URL 재진입 (폼/상태 리셋).

### Step 5b: API/MCP 호출 (curl — API 모드)

JSON 응답을 다루므로 `jq` 와 함께. 응답 헤더가 필요하면 `-i` 또는 `-D -`.

#### REST API 패턴

```bash
# 공개 GET — 상태 코드 + 본문 함께 보기
curl -sS -w "\nHTTP %{http_code}\n" http://localhost:$DEV_PORT/api/posts | jq .

# 헤더까지
curl -sSi http://localhost:$DEV_PORT/api/posts/1

# 인증 — Authorization 헤더 (토큰은 별도 변수, 본 파일에 박지 않음)
TOKEN="$(cat ~/.local/share/<project>/dev-token 2>/dev/null || echo '<set-token-here>')"
curl -sS -w "\nHTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:$DEV_PORT/api/posts

# POST — 정상 본문
curl -sS -w "\nHTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"hello","content":"world"}' \
  http://localhost:$DEV_PORT/api/posts | jq .

# Edge: 빈 title (검증 실패 기대)
curl -sS -w "\nHTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"","content":"x"}' \
  http://localhost:$DEV_PORT/api/posts | jq .

# Edge: 토큰 없음 (401 기대)
curl -sS -w "\nHTTP %{http_code}\n" \
  -H "Content-Type: application/json" \
  -d '{"title":"x","content":"y"}' \
  http://localhost:$DEV_PORT/api/posts
```

#### MCP (JSON-RPC) 패턴

MCP 서버는 보통 단일 엔드포인트에 JSON-RPC. 프록시·요약 도구가 응답을 가공해 진단을 어렵게 할 수 있어 `command curl` / `command grep` 으로 셸 빌트인 우회:

```bash
# 도구 목록
command curl -sS http://localhost:$DEV_PORT/api/mcp \
  -H "Authorization: Bearer $MCP_KEY" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' \
  | jq '.result.tools[] | {name, description}'

# 특정 도구 스키마
command curl -sS http://localhost:$DEV_PORT/api/mcp \
  -H "Authorization: Bearer $MCP_KEY" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' \
  | jq '.result.tools[] | select(.name=="<tool>") | .inputSchema'

# 도구 호출
command curl -sS http://localhost:$DEV_PORT/api/mcp \
  -H "Authorization: Bearer $MCP_KEY" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"<tool>","arguments":{...}}}' \
  | jq .
```

**증적**: `curl` 출력은 콘솔에 그대로 남고 사용자 가시. 필요하면 시나리오별로 `> /tmp/mt-<name>.json` 으로 저장해 표에 인용.

### Step 6: 정리

- UI 모드: `mcp__playwright__browser_close` — 페이지 닫기
- 모든 모드: `TaskStop` — 백그라운드 dev 서버 태스크 중단 (사용자가 "켜둬" 라고 하지 않은 한)
- `Monitor` 도 정리

### Step 7: 결과 보고 (표 형태)

UI 모드:
| 스크린샷 | 시나리오 | 결과 |
|---|---|---|
| `<name>-empty.png` | 초기 로드 | 폼 렌더, 빈 상태 |
| `<name>-success.png` | 정상 입력 | 리다이렉트 + 성공 메시지 |
| `<name>-<case>-error.png` | 오류 케이스 | 필드별/글로벌 오류 렌더 |

API 모드:
| 시나리오 | 메소드/경로 | 상태 코드 | 응답 요지 |
|---|---|---|---|
| posts-list | `GET /api/posts` | 200 | `content[]` 5건, `page.totalElements=5` |
| posts-create-unauth | `POST /api/posts` (no token) | 401 | `{"error":"Unauthorized"}` |
| posts-create-empty-title | `POST /api/posts` | 400 | `{"errors":[{"field":"title","message":"..."}]}` |

자동 테스트가 검증하는 계약과 실제 동작이 일치하는지 명시. 불일치 발견 시 즉시 보고하고, 테스트 계약과 구현/문구 중 어느 쪽을 맞출지 사용자에게 결정 요청.

## 가이드라인

### 공통
- **시나리오 승인 게이트가 최우선**: 어떤 자동 판단도 사용자 승인 위에 놓지 않는다.
- **edge case 누락은 RED 결함**: CLAUDE.md 체크리스트의 해당 항목이 시나리오 표에 없으면 — 사용자가 빼라고 한 게 아니라면 — 추가하고 재승인.
- **자격증명을 본 파일에 박지 마라**: 토큰·이메일·비밀번호는 환경변수(`$E2E_CRED_ENV`) 또는 사용자 즉석 발급.

### UI 모드
- **비헤드리스 기본**: Playwright MCP 기본 모드. 자동화 테스트(헤드리스) 와 분리.
- **엘리먼트 참조는 snapshot 에서만**: `browser_snapshot` 의 `ref=eN` 을 그대로 전달. CSS selector 추측 금지.
- **스크린샷 네이밍**: `<feature>-<state>.png` (예: `signup-empty.png`, `signup-success.png`, `signup-duplicate-error.png`).
- **한 시나리오는 한 턴에 몰아서**: navigate → snapshot → fill → click → screenshot. 중간 설명 최소화.

### API 모드
- **상태 코드 + 본문 둘 다 본다**: `-w "\nHTTP %{http_code}\n"` 또는 `-i` 로 함께 출력. 본문만 보고 200 으로 단정 금지.
- **민감 헤더 마스킹**: 토큰은 보고 표에 마스킹 (`Bearer ey...***`).
- **MCP 는 `command curl` 우회**: 셸이 curl 을 alias·proxy 로 가공하는 환경을 우회. `command grep` 도 동일 이유.
- **에러 메시지 형식 점검**: 검증 실패 응답이 사람·기계가 읽을 수 있는 구조인지 (예: `{errors:[{field,message}]}`). 자동 테스트가 그 형식을 어떻게 검증하는지와 일치하는가.

## 후속 제안

시연 중 자동화하면 좋은 케이스를 발견하면 다음 중 하나로 고정:
- 통합 테스트 추가 (빠르고 CI 기본 포함, MockMvc / supertest / requests 등)
- 브라우저 e2e 테스트 추가 (실제 브라우저, 옵트인 또는 nightly)

## 다른 스킬과의 관계

- **호출자**: `create-issue` (재현), `plan-issue` (재현), `raise-pr` (변경 PR 의 증적), `local-tests` (자동 검증을 보조)
- **호출**: `dev-server` (인프라 의존이 있거나 포트 폴백·헬스체크가 필요할 때)
- **범위 밖**: 자동 회귀 테스트 작성·실행, 서버 기동 자체의 디버깅, 부하 테스트
