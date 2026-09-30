---
description: Swagger API 문서(ControllerDocs 인터페이스) 작성 규칙
---

# API Docs Convention

Kotlin 문법(배열 인자 `[...]`, 중첩 어노테이션 `@` 생략, `::class`)은 `.claude/rules/code-convention/controller.md`의 Docs 인터페이스 절을 따른다.
이 문서는 **무엇을 적는지**를 정한다.

## 파일 구조

- 각 Controller마다 `controller/{Controller}Docs.kt` 인터페이스를 생성하라 (Controller와 같은 `controller/` 패키지)
- Controller는 이 인터페이스를 구현하라
- Swagger 어노테이션은 Docs 인터페이스에만 선언하라. Controller에는 붙이지 마라

## 어노테이션 규칙

### 인터페이스 레벨

| 어노테이션 | 규칙 | 예시 |
|---|---|---|
| `@Tag` | name: `"{Domain} API"`, description: 한글 요약 | `@Tag(name = "Post API", description = "게시글 관련 API")` |

### 함수 레벨

| 어노테이션 | 규칙 | 예시 |
|---|---|---|
| `@Operation` summary | 기능을 한 줄로 요약 | `"내 게시글 조회"` |
| `@Operation` description | 인증 필요 시 허용 세션 mode를 적는다. `"🔐 <strong>세션 필요 (ACTIVE)</strong><br>"` | - |
| 파라미터 어노테이션 | Controller 시그니처와 동일하게 선언 | - |

### 응답 (`@ApiResponses`)

| 구분 | description 형식 | 비고 |
|---|---|---|
| 성공 | `"✅ {성공 메시지}"` | - |
| 실패 | `"🚨 {에러 설명}"` | `schema = Schema(implementation = ErrorResponse::class)` |

- `ExampleObject`의 value는 **실제 응답 본문과 같은 형태**여야 한다.
  `ErrorResponse`는 `error` 아래에 `code`, `category`, `requestId`를 가지므로 형식은 `{"error" : {"code" : "ANSWER_NOT_FOUND", "category" : "VALIDATION", "requestId" : "req-example"}}`다.
  - `code`는 `ExceptionCode` enum 상수명 그대로다. `category`는 그 code에 고정된 값을 옮긴다
  - `message`를 적지 마라. 에러 응답에 자유 형식 메시지는 없다
  - `recovery`, `retryAfterSeconds`는 계약이 그 code에 정한 경우에만 적는다
- `ErrorResponse`는 `com.arca.global.exception.dto.response.ErrorResponse`를 사용하라
- 선언할 `ApiResponse`는 해당 엔드포인트에서 실제로 발생하는 응답만이다. 500은 선언하지 마라

### 파라미터

- 인증 파라미터 `@Auth authSession: AuthSession`은 Docs 인터페이스에도 동일하게 선언하라

## DTO @Schema

- Request/Response data class의 각 프로퍼티에 `@field:Schema`를 붙여라
- 속성이 2개 이상이면 속성당 한 줄로 쓴다 (`.claude/rules/code-convention/dto.md`)

## 인터페이스 골격

```kotlin
package com.arca.{domain}.controller

import com.arca.global.exception.dto.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@Tag(name = "{Domain} API", description = "{도메인} 관련 API")
interface {Controller}Docs {

    @Operation(
        summary = "{기능 요약}",
        description = "{상세 설명}<br>" +
            "🔐 <strong>세션 필요 ({허용 mode})</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ {성공 메시지}"),
        ApiResponse(
            responseCode = "404",
            description = "🚨 {에러 설명}",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "{에러명}",
                            value = "{\"error\" : {\"code\" : \"{코드}\", \"category\" : \"{카테고리}\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun methodName(
        @Auth authSession: AuthSession,
    ): ResponseEntity<{ResponseType}>
}
```
