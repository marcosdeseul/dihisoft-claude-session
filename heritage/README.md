# heritage — 여러 프로젝트에서 재사용할 CLAUDE.md + 스킬 모음

이 폴더는 **스택·도메인 의존성을 제거한** 개발 원칙(`CLAUDE.md`), 코드 리뷰 에이전트(`agents/pr-reviewer.md`), 라이프사이클 스킬 6개(`skills/*`)를 담고 있다. 새 프로젝트에 들어갈 때 이 폴더만 가져가서 어댑터 변수를 채우면 동일한 TDD-기반 워크플로우(plan → 구현 → 검증 → PR → 리뷰)가 즉시 가동된다.

---

## 폴더 구조

```
heritage/
├── README.md                          # 이 파일
├── CLAUDE.md                          # TDD + 작업 단위 + 커밋 규칙 (한국어, 중립화)
├── agents/
│   └── pr-reviewer.md                 # multi-agent-review 가 의존하는 단일 에이전트
└── skills/
    ├── dev-server/SKILL.md            # 로컬 dev 서버 백그라운드 기동 + 준비 대기
    ├── manual-test/SKILL.md           # Playwright MCP UI 시연·증적
    ├── multi-agent-review/SKILL.md    # 3관점 병렬 리뷰 + 교차 검증
    ├── raise-pr/SKILL.md              # 완료 브랜치 → push → CI → 머지 대기
    ├── create-issue/SKILL.md          # 버그 묘사 → 재현 → 이슈 생성 → 승인 → 구현
    ├── local-tests/SKILL.md           # lint → typecheck → test → coverage → 시각/E2E
    └── plan-issue/SKILL.md            # 이슈 URL → 재현 → 명확화 → TDD 플랜 → PR
```

**7 스킬 + 1 에이전트 + 1 CLAUDE.md.** test-coverage / clean-up-dev-envs / draft-ralph 는 의도적으로 제외 (다른 스킬과 90% 중복, 또는 사용자 레벨 운영, 플러그인 종속).

---

## 새 프로젝트에 적용하는 법

### 1. 복사 또는 심볼릭 링크

```bash
# A) 복사 (프로젝트마다 독립 진화)
cp -r heritage/.claude-style/skills <new-project>/.claude/skills
cp heritage/agents/pr-reviewer.md <new-project>/.claude/agents/pr-reviewer.md

# B) 심볼릭 링크 (heritage 가 단일 진실원천 — 변경 시 모든 프로젝트에 즉시 반영)
ln -s "$PWD/heritage/skills" <new-project>/.claude/skills
ln -s "$PWD/heritage/agents/pr-reviewer.md" <new-project>/.claude/agents/pr-reviewer.md
```

> **주의**: 위 경로 예시는 `<new-project>/.claude/skills` 형태를 가정한다. heritage 폴더 자체에는 `.claude` 가 없다 — 사용 시 직접 매핑하라.

### 2. CLAUDE.md 병합

새 프로젝트의 `CLAUDE.md` 가:
- **없다면**: `heritage/CLAUDE.md` 를 그대로 복사하고, 맨 위에 프로젝트 고유 섹션(스택, 도메인 모델, 디렉토리 구조)을 추가한다.
- **있다면**: heritage 의 "개발 철학", "Edge Case 체크리스트", "ZERO TOLERANCE", "커밋 규칙", "테스트 구조 기준", "작업 단위 제약", "프로젝트 어댑터" 섹션을 흡수한다. 중복은 heritage 쪽 표현으로 통일.

### 3. 어댑터 변수 채우기

heritage 의 모든 스킬은 빌드/테스트/CI 명령을 **변수**로 참조한다. 새 프로젝트의 `CLAUDE.md` 끝에 다음 표를 채워야 스킬이 정상 동작한다.

| 변수 | 의미 |
|---|---|
| `$DEV_START_CMD` | 로컬 dev 서버 기동 명령 |
| `$READY_PATTERN` | 기동 완료 로그 정규식 |
| `$DEV_PORT` | 기본 포트 |
| `$LINT_CMD` | 정적 검사 명령 |
| `$TYPECHECK_CMD` | 타입 검사 명령 (해당 시) |
| `$TEST_CMD` | 단위 테스트 명령 |
| `$COVERAGE_CMD` | 커버리지 측정 명령 |
| `$COVERAGE_THRESHOLD` | 커버리지 게이트 (도구·지표·값) |
| `$LOCAL_GATE_CMD` | "PR 직전 한 방" 게이트 |
| `$RUNTIME_PATH` | 언어 런타임 PATH (필요 시) |
| `$SOURCE_EXTS` | 변경 감지 정규식 |
| `$CI_TEST_JOB` | CI 테스트 잡 이름 |
| `$CI_BUILD_JOB` | CI 빌드 잡 이름 |
| `$E2E_CRED_ENV` | E2E 자격증명 환경변수 (선택) |
| `$STORYBOOK_CMD` | 시각 컴포넌트 빌드/테스트 (선택) |
| `$INFRA_CHECK_CMD` | 로컬 인프라 상태 확인 (선택, dev-server) |
| `$INFRA_START_CMD` | 로컬 인프라 기동 (선택, dev-server) |
| `$ENV_BOOTSTRAP_CMD` | 환경 파일 시드 명령 (선택, dev-server) |
| `$DEFAULT_BRANCH` | 메인 브랜치 이름 |

**예시 매핑** (이 worktree 의 dihisoft 프로젝트):

```
$DEV_START_CMD       = ./gradlew bootRun
$READY_PATTERN       = Started [A-Za-z]+Application
$DEV_PORT            = 8080
$LINT_CMD            = ./gradlew lint
$TYPECHECK_CMD       = (Java — compileJava 가 이를 대체. 별도 명령 없음)
$TEST_CMD            = ./gradlew test
$COVERAGE_CMD        = ./gradlew test jacocoTestReport jacocoTestCoverageVerification
$COVERAGE_THRESHOLD  = JaCoCo INSTRUCTION 95%
$LOCAL_GATE_CMD      = ./gradlew clean build
$RUNTIME_PATH        = export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
$SOURCE_EXTS         = \.java$
$CI_TEST_JOB         = Test + Coverage
$CI_BUILD_JOB        = Build (no tests)
$E2E_CRED_ENV        = (없음 — H2 매번 초기화로 즉석 발급)
$STORYBOOK_CMD       = (없음)
$INFRA_CHECK_CMD     = (없음 — H2 인메모리 사용)
$INFRA_START_CMD     = (없음)
$ENV_BOOTSTRAP_CMD   = (없음)
$DEFAULT_BRANCH      = main
```

**예시 매핑** (Next.js + Supabase 풀스택):

```
$DEV_START_CMD       = cd apps/web && PORT=3001 npm run dev
$READY_PATTERN       = ready in [0-9]+
$DEV_PORT            = 3001
$LINT_CMD            = cd apps/web && npm run lint
$TYPECHECK_CMD       = cd apps/web && npm run typecheck
$TEST_CMD            = cd apps/web && npm test
$COVERAGE_CMD        = cd apps/web && npm run test:coverage
$COVERAGE_THRESHOLD  = Jest patch 95%
$LOCAL_GATE_CMD      = cd apps/web && npm run lint && npm run typecheck && npm run test:coverage && npm run build
$RUNTIME_PATH        = (없음)
$SOURCE_EXTS         = \.tsx?$\|\.css$
$CI_TEST_JOB         = test
$CI_BUILD_JOB        = build
$E2E_CRED_ENV        = E2E_TEST_EMAIL,E2E_TEST_PASSWORD
$STORYBOOK_CMD       = cd apps/web && npm run build-storybook && npm run test-storybook:ci
$INFRA_CHECK_CMD     = cd apps/web && supabase status
$INFRA_START_CMD     = cd apps/web && supabase start
$ENV_BOOTSTRAP_CMD   = cd apps/web && cp -n .env.example .env.local
$DEFAULT_BRANCH      = main
```

---

## 의존

- `gh` CLI (인증 완료)
- Playwright MCP — `manual-test` 가 사용 (선택, UI 가 없는 프로젝트면 불필요)
- `pr-reviewer` 에이전트 — `multi-agent-review` 가 사용 (`heritage/agents/pr-reviewer.md` 를 프로젝트 또는 글로벌 `.claude/agents/` 에 배치)

---

## 라이프사이클 흐름

```
[버그/기능 묘사]
        │
        ▼
   create-issue ──► (이슈 #N 생성 + 사용자 승인 게이트)
        │   ├── (재현 필요 시) ──► dev-server ──► manual-test
        ▼
   plan-issue ──► (이슈 본문 + 재현 + 명확화 + TDD 플랜)
        │   ├── (재현 필요 시) ──► dev-server ──► manual-test
        ▼
   [TDD 구현 — RED → GREEN → REFACTOR, 커밋 분리]
        │
        ▼
   local-tests ──► (lint → typecheck → test → coverage → 시각/E2E)
        │
        ▼
   manual-test (UI 변경 시) ──► (스크린샷 증적)
        │   └── (서버 미기동 시 사전) ──► dev-server
        ▼
   raise-pr ──► (push + PR 본문 + CI 감시 + 실패 루프 + 이슈/PR 체크박스)
        │
        ▼
   multi-agent-review ──► (보안 + TDD + 아키텍처 병렬 + 교차 검증)
        │
        ▼
   [사용자가 머지]
```

이슈 URL 이 이미 있으면 `plan-issue` 부터 진입. 코드는 다 됐고 PR 만 올리면 되면 `raise-pr` 부터 진입.

---

## 일반화 원칙 (스킬 작성 시 참고)

heritage 에 새 스킬을 추가하거나 기존 스킬을 손볼 때 다음을 지킨다:

1. **명령은 변수로**: `npm run lint` / `./gradlew lint` 같은 구체 명령을 본문에 박지 말고 `$LINT_CMD` 로 참조.
2. **MCP 가정 제거**: Serena, Chakra UI MCP 등 특정 MCP 호출을 박지 않는다. 일반 `Read`/`Grep`/`Edit` + `Explore` 서브에이전트로 표현.
3. **자격증명·하드코딩 제거**: 이메일·비밀번호·특정 포트·특정 프로젝트 이름을 본문에 적지 않는다.
4. **언어 통일**: SKILL.md frontmatter `description` + 본문 모두 한국어. 트리거 문구는 한국어 + 영어 키워드 병기.
5. **CLAUDE.md 단일 진실원천**: 작업 단위 제약(≤10 파일), TDD 사이클, ZERO TOLERANCE 같은 규칙은 본문에 다시 적지 말고 `heritage/CLAUDE.md` 의 해당 섹션을 가리킨다.
6. **다른 스킬과의 관계 박스**: 각 SKILL.md 끝에 선행/호출/후속/범위밖 명시.

---

## 의도적으로 제외한 후보 스킬

| 스킬 | 제외 사유 |
|---|---|
| `test-coverage` | `local-tests` + `raise-pr` 와 90% 중복. 유지 부담만 증가 |
| `clean-up-dev-envs` | 사용자(`~`) 레벨 운영 스킬 (claude-squad worktree 정리). 프로젝트 경계와 무관 — `~/.claude/skills/` 가 적절 |
| `draft-ralph` | ralph-loop 플러그인 종속 |

---

## 변경 이력

- **초기 시드**: 풀스택(Next.js + Supabase) 프로젝트의 라이프사이클 스킬 10개와 백엔드(Spring + Gradle) 프로젝트의 스킬 3개를 비교하여, 도구·도메인 종속을 모두 제거하고 어댑터 변수로 추상화한 첫 큐레이션.
