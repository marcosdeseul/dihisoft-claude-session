---
name: dev-server
description: 로컬 dev 서버를 백그라운드로 기동하고 기동 완료 신호를 기다린 뒤 포트를 보고한다. 사용자가 "dev 서버 띄워줘", "start dev server", "run dev", "로컬 서버 기동", "서버 켜줘" 등으로 요청할 때 호출한다. 포트가 점유되어 있으면 후속 포트로 자동 폴백하고, 인프라 의존(예: 로컬 DB, 메시지 브로커) 이 정의되어 있으면 먼저 기동한다. 단순히 한 번 기동만 하고 끝나는 워크플로우 — UI 시연(`manual-test`) 이 후속으로 자주 따라온다.
---

# dev-server — 로컬 dev 서버 기동 + 준비 대기

`$DEV_START_CMD` 로 dev 서버를 백그라운드 실행하고, `$READY_PATTERN` 로그가 보일 때까지 기다린 뒤, 어떤 포트에서 떠 있는지 사용자에게 보고한다.

## 트리거
- "dev 서버 띄워줘", "start dev server", "run dev", "서버 켜줘", "로컬 서버 기동"
- UI 변경을 브라우저로 보고 싶을 때 (이후 `manual-test` 가 자연스럽게 이어진다)
- API 를 `curl` 로 시연할 로컬 환경이 필요할 때

## 전제

CLAUDE.md 의 어댑터 표에 다음이 정의되어 있어야 한다:

| 변수 | 필수 여부 | 메모 |
|---|---|---|
| `$DEV_START_CMD` | 필수 | 단, 포트는 `$DEV_PORT` 로 받을 수 있도록 변수화되어 있어야 한다 |
| `$READY_PATTERN` | 필수 | 기동 완료를 단정할 수 있는 정규식 |
| `$DEV_PORT` | 필수 | 기본 포트 — 점유 시 +1 로 폴백 |
| `$RUNTIME_PATH` | 선택 | 언어 런타임 PATH (Java 등) |
| `$INFRA_CHECK_CMD` | 선택 | 로컬 DB/브로커 상태 확인 (예: `supabase status`, `docker compose ps`) |
| `$INFRA_START_CMD` | 선택 | 로컬 인프라 기동 (예: `supabase start`, `docker compose up -d`) |
| `$ENV_BOOTSTRAP_CMD` | 선택 | `.env.local` 등 환경 파일 시드 (예: `cp .env.example .env.local`) |

선택 변수가 정의되지 않은 프로젝트에선 해당 단계가 자동 스킵된다 — "정의되지 않음" 으로 표시.

## 워크플로우

### Step 1: 사용 가능한 포트 탐색

기본 포트(`$DEV_PORT`) 부터 시작해 점유 시 +1 로 5번까지 폴백:

```bash
PORT=""
for p in $(seq $DEV_PORT $((DEV_PORT + 4))); do
  if ! lsof -i :$p > /dev/null 2>&1; then
    PORT=$p
    break
  fi
done
echo "selected port: $PORT"
```

- 사용자가 명시적으로 포트를 지시했다면 (`"3002에서 띄워줘"`) 그 포트만 시도하고 점유 시 사용자에게 보고.
- 다른 worktree/세션의 dev 서버가 같은 포트에 떠 있는 경우가 흔하다. **죽이지 말고 다른 포트로 폴백**.

### Step 2: 인프라 점검·기동 (정의되어 있을 때)

```bash
if [ -n "$INFRA_CHECK_CMD" ]; then
  $INFRA_CHECK_CMD || $INFRA_START_CMD
fi
```

`docker compose`, `supabase`, `postgres@brew` 같은 외부 서비스를 dev 서버가 의존하는 경우 사용. 기동 시간이 오래 걸리는 인프라는 별도 백그라운드로 띄우고 dev 서버 시작 전에 health check 가 통과해야 한다.

### Step 3: 환경 파일 시드 (필요 시)

```bash
if [ -n "$ENV_BOOTSTRAP_CMD" ]; then
  $ENV_BOOTSTRAP_CMD
fi
```

`.env.local` 같은 파일이 없을 때만 만든다. 이미 있으면 덮어쓰지 않는다 — 사용자의 로컬 시크릿이 날아갈 수 있다. 본 스킬은 시크릿 값을 본문에 박지 않는다.

### Step 4: dev 서버 기동 (백그라운드)

```bash
# 필요한 경우 런타임 PATH
$RUNTIME_PATH

# 포트가 변수화되어 있다면 명령에 그대로 전달
PORT=$PORT $DEV_START_CMD
```

- `run_in_background: true` 로 실행, 출력 파일 경로 확보.
- `$DEV_START_CMD` 가 포트 인자를 받지 못하면 어댑터 표에서 명시 — 그 경우 기본 포트만 사용 가능.

### Step 5: 기동 완료 대기

`Monitor` 로 출력 파일을 tail + grep (line-buffered):

```bash
tail -f <output-file> | grep -E --line-buffered \
  "$READY_PATTERN|APPLICATION FAILED|BUILD FAILED|Error:|EADDRINUSE|Port .* (already|in use)" | head -1
```

- 성공: `$READY_PATTERN` 매칭 → Step 6.
- 실패: 그 외 패턴 → 즉시 중단, 출력 파일 마지막 50줄을 사용자에게 보고.
- 타임아웃: 60~120초 (프로젝트별 첫 부팅 시간에 맞게). 그 안에 시그널이 안 보이면 사용자에게 상태 확인 요청.

### Step 6: 헬스체크 (선택)

URL 이 정의된 프로젝트면 한 번 호출해 200 응답 확인:

```bash
curl -sSf -o /dev/null -w "%{http_code}\n" http://localhost:$PORT/ || true
```

결과(2xx/3xx 면 정상)를 사용자에게 함께 보고.

### Step 7: 보고

```
✅ dev 서버 기동 완료
   URL: http://localhost:$PORT
   백그라운드 태스크 ID: <task-id>
   로그: <output-file>

   다음:
   - manual-test 스킬로 UI 시연
   - curl 로 API 호출
   - 종료: TaskStop <task-id>
```

## 가이드라인

- **다른 dev 서버를 죽이지 마라**: 동일 포트에 다른 worktree 가 떠 있으면 폴백 포트로. claude-squad 환경에서는 paused 세션도 활성으로 간주.
- **포그라운드 금지**: 항상 `run_in_background: true`. 포그라운드는 후속 Tool 호출을 차단한다.
- **대기는 `Monitor` 로**: `sleep` 체인 금지. 시그널 없이 임의 대기는 첫 부팅이 느린 프로젝트에서 깨진다.
- **시크릿 박지 마라**: `.env.local` 시드 명령은 `cp` 만 — 값을 본문에 박지 않는다. 시크릿이 필요한 경우 사용자에게 위임.
- **에러는 곧장 보고**: `BUILD FAILED`, `EADDRINUSE`, 의존성 미설치 등은 사용자가 즉시 결정해야 함. 자동 재시도 X.
- **헬스체크 실패는 정보 제공**: 200 이 아니어도 일부 dev 서버는 메인 페이지가 200 이 아닐 수 있다. 단정 짓지 말고 응답 코드만 보고.

## 다른 스킬과의 관계

- **호출자**: `manual-test` (Step 2 의 서버 기동을 위임), `plan-issue`/`create-issue` 의 재현 단계
- **호출**: 없음 (라이프사이클 끝단의 인프라 스킬)
- **후속**: `manual-test` (UI 시연), 또는 사용자가 직접 `curl`/브라우저 사용
- **범위 밖**: 프로덕션 배포 서버, CI 파이프라인 환경, dev 서버 자체의 디버깅(소스 변경 후 hot-reload 등은 dev 서버의 책임)
