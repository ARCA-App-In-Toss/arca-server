---
description: Kotlin 소스 코드를 작성하거나 수정할 때 공통으로 적용되는 컨벤션
paths:
  - "src/main/kotlin/**/*.kt"
---

# Common Code Convention

Kotlin 공식 코딩 컨벤션을 기본으로 하고, 겹치는 부분은 이 문서를 따른다.

## 소스 위치

- 소스는 `src/main/kotlin/com/arca/` 아래에 두고, 패키지 구조는 `project-structure.md`를 따른다
- 파일 하나에 최상위 클래스 하나를 두고, 파일명은 클래스명과 같게 하라
- 클래스 안에 클래스를 중첩하지 마라. 바깥 클래스에서만 쓰는 작은 타입이어도 별도 파일의 최상위 클래스로 둔다
  - JSON이 여러 단계인 DTO도 단계마다 파일을 나눈다 (`ErrorResponse`, `ErrorDetail`)
  - `companion object`는 중첩 클래스가 아니다
  - 나누면 오히려 읽기 어려워지는 경우에만 중첩하고, 먼저 사용자에게 알려라

## 레이어 구조

Controller → Service → Repository 순서를 따른다 (Facade 레이어 없음).
역방향 의존을 만들지 마라: Repository가 Service를 참조하면 안 된다.

## 불변과 null

- 기본은 `val`이다. 값이 바뀌어야 할 때만 `var`를 써라
- 타입은 non-null로 선언하고, null이 의미 있는 상태일 때만 `?`를 붙여라
- `!!`를 쓰지 마라. null을 예외로 바꿀 때는 `?: throw`를 쓴다
- Java API가 돌려주는 값(플랫폼 타입)은 받는 변수나 반환 타입에 nullability를 명시해 확정하라

## 예외 처리

- `throw RestApiException(XXX)` 패턴을 사용하라
- 에러 코드는 프론트와 합의한 API 계약의 닫힌 목록이다. `ExceptionCode` enum 상수명이 곧 응답의 `code` 값이다 (`SESSION_INVALID`, `ANSWER_NOT_FOUND`)
  - 계약에 없는 코드를 임의로 추가하지 마라. 필요하면 먼저 사용자에게 알리고 계약 변경으로 다룬다
- `ExceptionCode` 항목은 HTTP 상태와 category(`VALIDATION`, `AUTH`, `CONFLICT`, `RATE_LIMIT`, `MAINTENANCE`)를 가진다. code별 조합은 계약에 고정돼 있다
- `ExceptionCode`는 category별 주석 그룹을 유지하라 (`// 인증 (AUTH)`)
- 에러 응답은 `{"error": {"code", "category", "requestId"}}` 형태다. `message`, `details`, stack 같은 자유 형식 필드를 넣지 마라. 사용자에게 보일 문구는 클라이언트가 code로 정한다
  - `recovery`는 계약이 정한 code에만 붙인다. 런타임 값(`ticketId`, 정책 목록)이 필요하면 `RestApiException`에 실어 던진다
  - `retryAfterSeconds`는 `RATE_LIMITED`, `MAINTENANCE`에만 붙이고 `Retry-After` 헤더와 같은 값을 쓴다
- 요청 형식 오류(JSON 파싱, 필수 필드 누락, 타입 불일치, 추가 필드, Bean Validation 실패)는 모두 `400 INVALID_REQUEST`다
- command가 적용되지 않은 결과(`NOT_APPLIED`)는 예외가 아니다. `200` 응답 body의 `state`와 `error`로 반환하라
- `ExceptionCode` 항목은 개별 import로 식별자만 노출하라 (`ExceptionCode.XXX` 표기 대신 `XXX`)
  (`import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND`)
  - 단 `ExceptionCode` 타입 자체를 참조할 때(파라미터 타입 등)는 타입을 import한다 (예: `GlobalExceptionHandler`)
- 와일드카드 import(`*`)를 쓰지 마라
- 비즈니스 검증에 `require()`/`check()`를 쓰지 마라. 이들이 던지는 `IllegalArgumentException`/`IllegalStateException`은 `GlobalExceptionHandler`에서 `500 INTERNAL_ERROR`로 떨어진다
  - 요청 값과 무관하게 코드를 잘못 짠 경우만 잡는 검사에는 `check()`를 쓴다 (`RestApiException`의 code와 recovery 짝 검사). 이 실패는 서버 버그이므로 500이 맞다
  - `recovery`가 필수인 code를 `recovery` 없이, 또는 다른 kind로 던지면 `RestApiException` 생성에서 실패한다. code별 kind는 `ExceptionCode.exceptionRecoveryKind`에 있다

```kotlin
val member = memberRepository.findByIdOrNull(memberId)
    ?: throw RestApiException(MEMBER_NOT_FOUND)
```

## 객체 생성

- 도메인 객체(Entity)는 `private constructor` + `companion object`의 팩토리 함수(`create()` / `of()` / `from()`)로만 생성하라
- Response DTO도 팩토리 함수로 생성하라. 단 data class라 생성자는 public으로 둔다 (`dto.md`)
- 인자가 2개 이상인 생성자, 팩토리 호출은 이름 붙인 인자로 한 줄에 하나씩 작성하라

```kotlin
fun create(
    member: Member,
    title: String,
): Post {
    return Post(
        member = member,
        title = title,
    )
}
```

## Lombok 금지

Lombok을 쓰지 마라. 아래처럼 Kotlin 문법으로 대신한다.

| Lombok | Kotlin |
|---|---|
| `@Getter` | 프로퍼티 (`val` / `var`) |
| `@RequiredArgsConstructor` | 주 생성자 |
| `@NoArgsConstructor` (Entity) | `kotlin-jpa` 플러그인 |
| `@Builder` | 이름 붙인 인자 |
| `@UtilityClass` | `object` |
| `@Log4j2` / `@Slf4j` | 아래 로깅 규칙 |

## 상수

- 매직넘버, 매직스트링을 코드에 직접 쓰지 말고 상수로 선언하라
- 상수는 `companion object` 안에 `private const val`로 선언하라. `object`면 그 본문에 둔다
  - 두 클래스가 같은 값을 맞춰 써야 하면 값을 정하는 쪽에 `const val`로 공개하고 다른 쪽이 import한다 (`RequestIdFilter.REQUEST_ID_ATTRIBUTE`). 같은 문자열을 양쪽에 따로 두지 마라
  - 파일 최상단(클래스 선언 밖)에 두지 마라. 파일을 열었을 때 클래스 선언이 먼저 보여야 한다
  - enum이면 companion 안에서 항목을 이름만으로 참조할 수 있다
- `const`가 불가능한 타입(`DateTimeFormatter`, 컬렉션)은 `private val`로 선언하라
- `companion object` 안에서는 상수와 로거를 먼저, 함수를 뒤에 둔다
- 이름은 `SCREAMING_SNAKE_CASE`로 작성하라

```kotlin
@Service
class PostService(
    private val postRepository: PostRepository,
) {
    private fun validatePostLimit(posts: List<Post>) {
        if (posts.size >= MAX_POST_COUNT) {
            throw RestApiException(POST_LIMIT_EXCEEDED)
        }
    }

    companion object {
        private const val MAX_POST_COUNT = 10
    }
}
```

## 포맷팅

- 들여쓰기는 4칸, 그 밖의 형식은 Kotlin 공식 스타일을 따른다
- 파라미터가 2개 이상인 함수, 생성자 선언은 파라미터마다 줄바꿈하고, 마지막 파라미터 뒤에 trailing comma를 붙여라
  - Controller와 Docs의 함수는 파라미터가 1개여도 줄바꿈한다 (`controller.md`)
- 의존성 주입 파라미터는 계층별로 묶고, 그룹 사이를 빈 줄로 나눠라
  - 같은 계층(Repository끼리, Service끼리)은 한 그룹이다. 도메인이 달라도 나누지 않는다. 그룹 안에서는 자기 도메인을 먼저 쓴다
  - 계층에 속하지 않는 컴포넌트(토큰 발급기, 인코더, 리졸버, 외부 API 클라이언트 등)는 따로 한 그룹으로 묶는다
  - 가장 가까운 아래 계층 그룹을 맨 위에 둔다. Service는 Repository 그룹, Controller는 Service 그룹이 먼저다

```kotlin
@Service
class AuthService(
    private val memberRepository: MemberRepository,

    private val tokenGenerator: TokenGenerator,
    private val passwordEncoder: PasswordEncoder,
)
```

- 함수 본문은 한 줄이어도 블록(`{ }`)으로 쓰고 `return`으로 반환하라. `=` 표현식 본문을 쓰지 마라. 커스텀 getter도 같다 (`get() { return ... }`)
  - Kotlin 공식 스타일은 한 줄 본문에 `=`를 권하지만, 이 프로젝트는 두 형태가 섞이지 않게 블록으로 통일한다
- 클래스 멤버 순서는 프로퍼티 → `init` 블록 → 부 생성자 → 함수 → `companion object`다 (Kotlin 공식 순서)
- 문자열 연결 대신 문자열 템플릿(`"$value"`, `"${post.title}"`)을 써라

## 함수 본문 구성

함수 본문에서 논리 단계나 처리 대상 도메인이 바뀌면 빈 줄로 구분해 맥락을 드러내라.

```kotlin
val member = memberRepository.findByIdOrNull(memberId)
    ?: throw RestApiException(MEMBER_NOT_FOUND)

val posts = postRepository.findByMemberId(member.id)

return PostsResponse.of(...)
```

## 함수와 컬렉션

- Java Stream 대신 Kotlin 컬렉션 함수(`map`, `filter`, `any`, `associateBy`, `groupBy`)를 써라
- 외부에 노출하는 컬렉션은 읽기 전용 타입(`List`, `Map`)으로 선언하라
- 분기가 3개 이상이면 `if-else` 체인 대신 `when`을 써라
- 범위 함수(`let`, `apply`, `also`, `run`, `with`)는 null 처리(`?.let`)와 객체 초기화에만 쓰고, 중첩하지 마라
- 상태 없는 유틸리티는 `object`로 선언하라

## 네이밍

- 패키지는 도메인 단위로 나눠라
- 클래스는 PascalCase로 작성하라 (`MemberService`, `PostController`)
- 함수는 camelCase + CRUD 동사를 사용하라 (`findByMemberId`, `createPost`, `deletePost`)
- API 경로는 `/v1` 아래에 kebab-case로 작성하라. 리소스 이름과 단복수는 API 계약을 그대로 따른다 (`/v1/passenger`, `/v1/answers`, `/v1/answer-write-commands`)
- 연속된 대문자를 쓰지 마라 (`lastSemesterGPA` 대신 `lastSemesterGpa`, `userID` 대신 `userId`)

### 클래스 타입 변수는 타입명을 그대로 써라

타입이 클래스나 enum인 프로퍼티, 파라미터, 지역 변수의 이름은 타입명을 줄이지 않고 camelCase로 옮겨 쓴다.
`exception`과 `recovery`처럼 타입명의 일부만 잘라 쓰지 마라.

```kotlin
// 지양
val code: ExceptionCode
val recovery: ExceptionRecovery?
val status: HttpStatus
@Auth session: AuthSession

// 지향
val exceptionCode: ExceptionCode
val exceptionRecovery: ExceptionRecovery?
val httpStatus: HttpStatus
@Auth authSession: AuthSession
```

- 프레임워크 타입과 override한 함수의 파라미터도 같다 (`httpServletRequest: HttpServletRequest`, `corsRegistry: CorsRegistry`)
- 값 타입(`String`, 숫자, `Boolean`, 날짜와 시간, 컬렉션)은 대상이 아니다. 역할을 드러내는 이름을 쓴다 (`title: String`, `createdAt: Instant`)
- 같은 타입의 변수가 한 범위에 둘 이상이면 타입명 앞에 수식어를 붙여 구분한다 (`sourcePost: Post`, `targetPost: Post`)

예외는 둘이다.

1. **Request, Response DTO는 `request`, `response`로 줄여 쓴다.** 타입명이 길어 그대로 옮기면 읽기 어렵다 (`request: CreatePostRequest`)
2. **API 계약이 정한 DTO 프로퍼티명은 계약을 따른다.** 계약이 `error.code`, `error.category`로 정했으면 `ErrorResponse`의 프로퍼티는 `code`, `category`다 (`dto.md`)

### 필드명에 클래스명을 반복하지 마라

`Post.postTitle`은 `post.postTitle`처럼 같은 말을 두 번 하게 만든다.
소속이 이미 타입으로 드러나므로 필드명에서 뺀다.
대상은 값 타입 필드다. 타입이 클래스나 enum인 필드는 위 규칙대로 타입명을 그대로 쓴다.

```kotlin
// 지양
class Post {
    var postTitle: String
    var status: PostStatus
}

// 지향
class Post {
    var title: String
    var postStatus: PostStatus
}
```

응답 DTO도 같다. `PostResponse`가 표현하는 대상이 게시글이므로 `postTitle`이 아니라 `title`이다.

예외는 셋이다.

1. **다른 엔티티에서 온 값은 출처를 밝힌다.** `PostResponse.memberNickname`은 게시글이 아니라 회원의 값이다
2. **같은 종류의 필드가 둘 이상이면 수식어를 남긴다.** 코드가 둘(내부 코드, 외부 연동 코드)이면 한쪽만 `code`로 줄였을 때 어느 쪽인지 알 수 없다
3. **API 계약이 정한 DTO 필드명은 계약을 따른다.** 계약이 `PassengerProfile.passengerCode`, `answerId`로 정했으면 그대로 쓴다 (`dto.md`)

### enum 타입명은 도메인 접두사를 유지하라

**타입명에서는 접두사를 뺄 수 없다.**
`PostStatus`(게시글 상태)와 `MemberStatus`(회원 상태)처럼 도메인별로 같은 개념이 공존해서
접두사가 없으면 이름이 충돌한다.

```kotlin
var postStatus: PostStatus
var memberStatus: MemberStatus
```

## 주석

- 메인 코드에 설명 주석을 달지 마라. 설명이 필요하다고 느끼면 주석 대신 이름과 구조로 드러내라
- 유지하는 예외: `ExceptionCode`의 카테고리 그룹 주석(`// 인증 (AUTH)`)처럼 나열을 구획하는 용도의 주석
- 배경과 정책 설명이 필요하면 주석이 아니라 `.claude/spec/service-policy/`의 해당 도메인 파일에 남겨라

## 로깅

- 로거는 `companion object`에 Log4j2 API로 선언하라

```kotlin
companion object {
    private val log = LogManager.getLogger(GlobalExceptionHandler::class.java)
}
```

## 스프링 컴포넌트

- 의존성은 주 생성자의 `private val` 파라미터로 주입하라. `lateinit var` 필드 주입을 쓰지 마라
- 스프링 빈 클래스는 `kotlin-spring` 플러그인이 `open`으로 만든다. 직접 `open`을 붙이지 마라
- `@Value`의 `$`는 문자열 템플릿과 겹치므로 `\$`로 이스케이프하라 (`@Value("\${cors.allowed-origin}")`)
- 설정 묶음은 `@ConfigurationProperties` + data class로 선언하라
