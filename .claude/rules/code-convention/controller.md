---
description: Controller 레이어와 Docs 인터페이스 작성 패턴
paths:
  - "src/main/kotlin/**/controller/**/*.kt"
---

# Controller Convention

문서 내용(어노테이션에 무엇을 적는지)은 `.claude/spec/api-docs-convention.md`를 따른다.

- `@RestController` 클래스의 주 생성자로 Service를 주입하고 `{Controller}Docs`를 구현하라
- 기본 경로는 `@RequestMapping("/v1/{리소스}")`으로 설정하라. 리소스 이름은 API 계약의 경로를 그대로 쓴다 (`/v1/passenger`, `/v1/answers`)
- Docs 인터페이스의 함수는 `override fun`으로 구현하라
- Controller에 비즈니스 로직을 넣지 마라. Service에 위임만 하라
- 인증된 세션은 커스텀 `@Auth` 어노테이션으로 주입받아라 (`@Auth authSession: AuthSession`). 세션은 mode와 승객을 담는다
- 엔드포인트가 허용하는 세션 mode는 계약에 정해져 있다. 허용 밖의 mode는 `403 SESSION_SCOPE_INSUFFICIENT`다
- RequestParam 검증이 필요하면 커스텀 검증 어노테이션(길이 제한, enum 값 검증)을 `global/annotation/`에 두고 사용하라
- `required = false`인 `@RequestParam`, `@RequestHeader`는 nullable 타입으로 받아라

## 요청 이름

- 요청 이름은 API 계약의 표기를 그대로 따른다
- 경로는 케밥 케이스로 써라 (`/event-batches`)
- 경로 변수와 쿼리 파라미터는 카멜 케이스다 (`{answerId}`, `excerptProfile`). Kotlin 파라미터 이름과 같게 짓고 이름을 따로 명시하지 않는다 (`@PathVariable answerId: String`)
- 헤더는 HTTP 표기 그대로 쓰고 이름을 명시하라 (`@RequestHeader("Accept-Language") acceptLanguage: String`)

## 함수 형식

- 파라미터는 개수와 상관없이 한 줄에 하나씩 쓰고, 마지막 파라미터 뒤에 쉼표를 붙이지 않는다. 파라미터가 없으면 `()`로 쓴다
- 파라미터 하나에 붙는 어노테이션은 그 파라미터와 같은 줄에 써라 (`@RequestParam keyword: String,`)
- 본문은 블록(`{ }`)으로 쓰고, 서비스 호출 결과를 `val response`에 담아 바로 다음 줄에서 반환하라. 두 줄 사이에 빈 줄을 넣지 않는다
- body가 없으면 서비스를 호출한 다음 줄에서 반환하라. 반환 타입은 `ResponseEntity<Void>`다

## 응답

- 응답은 항상 `ResponseEntity.status(상태).body(response)`로 만들어라. body가 없으면 `.status(상태).build()`다
  - `ok()`, `noContent()` 같은 단축 함수를 쓰지 마라. 상태 코드가 모든 함수에서 같은 자리에 보이게 한다
- 상태는 `HttpStatus` 항목을 하나씩 import해서 써라 (`import org.springframework.http.HttpStatus.OK`). 와일드카드 import는 쓰지 않는다

```kotlin
@RestController
@RequestMapping("/v1/posts")
class PostController(
    private val postService: PostService
) : PostControllerDocs {

    @GetMapping
    override fun getMyPosts(
        @Auth authSession: AuthSession
    ): ResponseEntity<PostsResponse> {
        val response = postService.getMyPosts(authSession)
        return ResponseEntity.status(OK).body(response)
    }

    @DeleteMapping("/{postId}")
    override fun deletePost(
        @Auth authSession: AuthSession,
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        postService.deletePost(authSession, postId)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}
```

## Docs 인터페이스

- Swagger 어노테이션은 Docs 인터페이스에만 선언하라. Controller에는 붙이지 마라
- 함수가 하나여도 `fun interface`로 선언하지 마라. 컨트롤러 클래스가 구현하고 springdoc이 어노테이션을 읽는 용도라 람다로 구현할 일이 없다
- 배열 인자는 `[...]`로, 중첩 어노테이션은 `@` 없이 작성하라
- 클래스 참조는 `ErrorResponse::class`로 작성하라
- 어노테이션 인자는 컴파일 타임 상수여야 한다. 긴 설명은 `+`로 이어 붙이고 `trimIndent()` 같은 함수 호출을 쓰지 마라
- 함수 선언은 컨트롤러와 같은 형식으로 써라 (파라미터마다 줄바꿈, 파라미터 어노테이션은 한 줄)

```kotlin
@Operation(
    summary = "게시글 삭제",
    description = "내가 작성한 게시글을 삭제합니다.<br>" +
        "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
)
@ApiResponses(
    ApiResponse(responseCode = "204", description = "✅ 게시글 삭제 성공"),
    ApiResponse(
        responseCode = "404",
        description = "🚨 게시글 조회 실패",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = [
                    ExampleObject(
                        name = "게시글 조회 실패",
                        value = "{\"error\" : {\"code\" : \"POST_NOT_FOUND\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                    )
                ],
                schema = Schema(implementation = ErrorResponse::class)
            )
        ]
    )
)
fun deletePost(
    @Auth authSession: AuthSession,
    @PathVariable postId: Long
): ResponseEntity<Void>
```
