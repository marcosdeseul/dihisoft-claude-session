# dihisoft-claude-session

Java Spring 기반으로 **회원가입 · 로그인 · 게시글 CRUD**를 제공하는 간단한 REST API 게시판.
학습/데모 목적으로 최소 구성으로 설계했으며, 실제 구현은 이후 단계에서 진행한다.

> **현재 단계**: 스펙 정의 — 이 README에 명세된 내용을 기반으로 이후 실제 코드가 작성됩니다.

---

## 1. 기술 스택

| 카테고리 | 선택 | 비고 |
|---|---|---|
| 언어 | Java 21 | LTS |
| 프레임워크 | Spring Boot 3.3.x | Web, Data JPA, Security, Validation |
| 빌드 도구 | Gradle (Kotlin DSL) | `build.gradle.kts` |
| 데이터베이스 | H2 (인메모리) | 개발/학습용, 재시작 시 초기화 |
| ORM | Spring Data JPA (Hibernate) | `ddl-auto=create-drop` |
| 인증 | Spring Security + JWT | Stateless, HS256 서명 |
| 비밀번호 해싱 | BCrypt | `BCryptPasswordEncoder` |
| 직렬화 | Jackson | Spring Boot 기본 |
| 테스트 | JUnit 5, Spring Security Test, MockMvc | 통합 테스트 중심 |

---

## 2. 기능 요구사항

### 인증
- **회원가입**: `username`(3–20자, 고유) + `password`(최소 6자). 비밀번호는 BCrypt 해시로 저장.
- **로그인**: `username` + `password` 검증 후 JWT 액세스 토큰 발급.
- **로그아웃**: 서버 상태 없음(stateless) — 클라이언트에서 토큰 폐기. (별도 엔드포인트 없음)

### 게시판
- **목록 조회**: 페이지네이션 지원(`page`, `size`). 공개.
- **단건 조회**: 공개.
- **작성**: 인증 사용자. 글의 `author`는 토큰의 사용자로 자동 지정.
- **수정 / 삭제**: **작성자 본인만** 가능. 타인이 시도하면 403.

### 권한 요약

| 작업 | 권한 |
|---|---|
| 조회(목록/단건) | 공개 |
| 작성 | 인증 |
| 수정/삭제 | 인증 + 작성자 본인 |

---

## 3. 도메인 모델

### `User`
| 필드 | 타입 | 제약 |
|---|---|---|
| `id` | `Long` | PK, auto |
| `username` | `String` | unique, not null, 3–20자 |
| `password` | `String` | not null, BCrypt 해시 저장 |
| `createdAt` | `Instant` | auto |

### `Post`
| 필드 | 타입 | 제약 |
|---|---|---|
| `id` | `Long` | PK, auto |
| `title` | `String` | not null, 1–100자 |
| `content` | `String` (TEXT) | not null |
| `author` | `User` | `@ManyToOne`, not null |
| `createdAt` | `Instant` | auto |
| `updatedAt` | `Instant` | auto |

### 관계
- `User 1 — N Post` (한 사용자가 여러 게시글 작성)

---

## 4. API 명세

Base URL: `http://localhost:8080`

### 4.1 인증

#### POST `/api/auth/signup` — 회원가입 (공개)
**Request**
```json
{ "username": "alice", "password": "pw12345" }
```
**Response 201**
```json
{ "id": 1, "username": "alice" }
```
**Errors**: 400 (검증 실패 / username 중복)

#### POST `/api/auth/login` — 로그인 (공개)
**Request**
```json
{ "username": "alice", "password": "pw12345" }
```
**Response 200**
```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 3600 }
```
**Errors**: 401 (자격 증명 실패)

### 4.2 게시글

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/posts?page=0&size=10` | 공개 | 목록 (페이지네이션) |
| GET | `/api/posts/{id}` | 공개 | 단건 조회 |
| POST | `/api/posts` | 인증 | 작성 |
| PUT | `/api/posts/{id}` | 인증 + 본인 | 수정 |
| DELETE | `/api/posts/{id}` | 인증 + 본인 | 삭제 |

#### GET `/api/posts` (목록)
**Response 200**
```json
{
  "content": [
    {
      "id": 10,
      "title": "첫 글",
      "content": "안녕하세요",
      "authorUsername": "alice",
      "createdAt": "2026-04-21T06:00:00Z",
      "updatedAt": "2026-04-21T06:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

#### POST `/api/posts` (작성)
**Request**
```json
{ "title": "첫 글", "content": "안녕하세요" }
```
**Header**: `Authorization: Bearer <token>`
**Response 201**
```json
{
  "id": 10,
  "title": "첫 글",
  "content": "안녕하세요",
  "authorUsername": "alice",
  "createdAt": "2026-04-21T06:00:00Z",
  "updatedAt": "2026-04-21T06:00:00Z"
}
```
**Errors**: 400 (검증), 401 (토큰 없음/만료)

#### PUT `/api/posts/{id}` (수정)
**Request**
```json
{ "title": "수정된 제목", "content": "수정된 본문" }
```
**Errors**: 401, 403 (본인 아님), 404

#### DELETE `/api/posts/{id}` (삭제)
**Response 204**
**Errors**: 401, 403, 404

---

## 5. 인증 흐름

1. 클라이언트가 `POST /api/auth/login` 호출.
2. 서버는 `username`/`password`를 검증(BCrypt)하고, 성공 시 **HS256 서명 JWT** 발급.
   - Payload: `sub=username`, `iat`, `exp` (만료 1시간).
3. 이후 보호된 엔드포인트는 `Authorization: Bearer <token>` 헤더 필수.
4. 서버의 `JwtAuthenticationFilter`가 토큰을 파싱·검증하여 `SecurityContext`에 인증 정보 주입.
5. 토큰 만료 시 401 응답 → 클라이언트는 재로그인.

> 리프레시 토큰, 비밀번호 재설정, 이메일 인증 등은 **범위 밖**.

---

## 6. 에러 응답 규약

모든 에러는 다음 형식의 JSON으로 응답:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "username이 이미 사용 중입니다",
  "path": "/api/auth/signup",
  "timestamp": "2026-04-21T06:00:00Z"
}
```

| HTTP | 의미 | 대표 케이스 |
|---|---|---|
| 400 | Bad Request | 검증 실패, username 중복 |
| 401 | Unauthorized | 토큰 없음/만료, 로그인 실패 |
| 403 | Forbidden | 타인의 글 수정/삭제 시도 |
| 404 | Not Found | 존재하지 않는 게시글 |
| 500 | Internal Server Error | 서버 예외 (버그) |

---

## 7. 프로젝트 구조 (예정)

```
.
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew, gradlew.bat, gradle/wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/example/board/
│   │   │   ├── BoardApplication.java
│   │   │   ├── config/          # SecurityConfig, JwtAuthenticationFilter
│   │   │   ├── security/        # JwtTokenProvider, CustomUserDetailsService
│   │   │   ├── user/            # User, UserRepository, AuthController, AuthService, dto/
│   │   │   ├── post/            # Post, PostRepository, PostController, PostService, dto/
│   │   │   └── common/          # GlobalExceptionHandler, ErrorResponse
│   │   └── resources/
│   │       └── application.yml
│   └── test/java/com/example/board/
│       ├── user/AuthControllerTest.java
│       └── post/PostControllerTest.java
└── README.md
```

---

## 8. 실행 방법 (예정)

> 아래는 구현 완료 후 기준. 현재 저장소에는 아직 코드가 없다.

```bash
./gradlew bootRun
```

- 서버: `http://localhost:8080`
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:board`)

### 빠른 테스트 (curl)
```bash
# 회원가입
curl -X POST localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"pw12345"}'

# 로그인 → 토큰
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"pw12345"}' | jq -r .accessToken)

# 게시글 작성
curl -X POST localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"hello","content":"first post"}'

# 목록 조회
curl localhost:8080/api/posts
```

---

## 9. 로드맵

- [x] **Step 1**: 스펙 정의 및 저장소 초기화 ← **현재 단계**
- [ ] Step 2: Gradle 프로젝트 스켈레톤 (`build.gradle.kts`, wrapper)
- [ ] Step 3: 도메인 엔티티 및 Repository
- [ ] Step 4: Spring Security + JWT 인증 구현
- [ ] Step 5: 인증 API (회원가입 / 로그인)
- [ ] Step 6: 게시글 CRUD API + 권한 검증
- [ ] Step 7: 전역 예외 처리
- [ ] Step 8: 통합 테스트
- [ ] Step 9: (선택) GitHub Actions CI

---

## 라이선스

MIT License — [LICENSE](LICENSE) 참고.
