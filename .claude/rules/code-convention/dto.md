---
description: DTO(Request/Response/internal) 클래스 작성 패턴
paths:
  - "src/main/kotlin/**/dto/**/*.kt"
---

# DTO Convention

- DTO는 `data class`로 선언하고 프로퍼티는 모두 `val`로 선언하라
- 생성자는 public으로 둔다. data class에 `private constructor`를 쓰면 `copy()`가 생성자를 우회하는 통로가 되어 컴파일러가 지적한다
- 패키지는 `{domain}/dto/request/`, `{domain}/dto/response/`, `{domain}/dto/internal/`로 나눈다
- 프로퍼티명에 그 DTO가 표현하는 대상의 이름을 반복하지 마라
  (`PostResponse.postTitle` 대신 `title`. 자세한 규칙과 예외는 `common.md`)
- **같은 값은 모든 DTO에서 같은 이름을 써라.** 한쪽은 `status`, 다른 쪽은 `postStatus`처럼 갈리면
  클라이언트가 응답마다 다르게 파싱해야 한다

## 프로퍼티 사이 빈 줄

- `request/`, `response/`는 프로퍼티 사이를 빈 줄로 구분하라. 프로퍼티마다 `@Schema`와 validation 어노테이션이 붙어서 붙여 쓰면 경계가 보이지 않는다
- `internal/`은 어노테이션이 붙지 않으니 빈 줄 없이 붙여 써라

## 프로퍼티 어노테이션

- 주 생성자 프로퍼티에 붙이는 `@Schema`와 validation 어노테이션은 `@field:` 대상을 명시하라 (대상을 적지 않으면 컴파일러 설정에 따라 붙는 자리가 달라진다)
- `@Schema` 속성이 2개 이상이면 한 줄에 몰아쓰지 말고 속성당 한 줄로 작성하라. 속성이 1개면 한 줄로 써도 된다

## 필드명

- JSON 필드명은 API 계약의 이름을 그대로 써라 (`memberCode`, `items`, `nextCursor`, `consentPolicies`)
- 계약과 이 문서의 네이밍 규칙이 어긋나면 계약이 우선이다

## Request

- `@Schema` + validation 어노테이션(`@NotBlank` 등)을 포함하라
- validation 어노테이션에 `message`를 쓰지 마라. 형식 검증 실패는 모두 `400 INVALID_REQUEST`로 나가고, 에러 응답에는 메시지가 담기지 않는다
- 프로퍼티는 계약의 필수 여부에 맞춰 선언하라. 필수 필드는 non-null, 계약이 `null`을 허용하는 필드만 nullable이다
  - 필드 누락과 타입 불일치는 역직렬화 단계에서 `INVALID_REQUEST`가 된다
  - 계약에 없는 추가 필드는 전역 Jackson 설정이 거절한다. DTO에서 따로 처리하지 않는다
- Bean Validation은 형식 검증(`400`)에만 써라. 계약이 `422`로 정한 값 규칙(닉네임 규칙, 본문 길이 등)은 Service나 도메인에서 `RestApiException`으로 처리하라

```kotlin
data class CreatePostRequest(
    @field:Schema(
        description = "게시글 제목",
        example = "첫 번째 글"
    )
    @field:NotBlank
    @field:Size(max = 50)
    val title: String,

    @field:Schema(
        description = "게시글 본문",
        example = "본문 내용"
    )
    @field:NotBlank
    val content: String
)
```

## Response

- 성공 응답은 wrapper 없이 DTO를 그대로 반환한다 (`{"data": ...}` 금지)
- 프로퍼티는 실제 값에 맞춰 non-null로 선언하라. 계약이 `T | null`로 정한 필드만 nullable이다
- 시각은 `Instant`(UTC, `Z`로 끝남), KST 날짜는 `LocalDate`(`YYYY-MM-DD`)로 선언하라. `LocalDateTime`을 쓰지 마라
- validation 어노테이션을 붙이지 마라. validation은 Request 전용이다
- 생성은 `companion object`의 `of()` / `from()`으로 하고, 그 안에서 이름 붙인 인자로 생성자를 호출하라

```kotlin
data class PostsResponse(
    val items: List<PostResponse>
) {
    companion object {
        fun of(items: List<PostResponse>): PostsResponse {
            return PostsResponse(items = items)
        }
    }
}
```

## internal

여러 조회에서 공유하거나 Repository projection, 캐시로 쓰는, 클라이언트에 그대로 나가지 않는 DTO를 둔다.

- 클래스 이름은 `~Dto`로 끝내라 (`PostCountDto`, `CachedPostDto`). `request/`, `response/`의 DTO에는 붙이지 마라
- JPQL 생성자 표현식(`SELECT new ...`)으로 받는 DTO는 생성자 파라미터의 타입과 순서를 select 절과 맞춰라. 클래스를 옮기면 쿼리 문자열의 패키지, 클래스 이름도 함께 고쳐라
