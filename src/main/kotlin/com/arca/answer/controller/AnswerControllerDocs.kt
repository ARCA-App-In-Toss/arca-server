package com.arca.answer.controller

import com.arca.answer.dto.request.CreateAnswerRequest
import com.arca.answer.dto.request.UpdateAnswerRequest
import com.arca.answer.dto.response.AnswerDetailResponse
import com.arca.answer.dto.response.AnswerPageResponse
import com.arca.answer.dto.response.CreateAnswerResponse
import com.arca.answer.dto.response.DeleteAnswerResponse
import com.arca.answer.dto.response.UpdateAnswerResponse
import com.arca.auth.domain.AuthSession
import com.arca.global.annotation.Auth
import com.arca.global.exception.dto.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Answer API", description = "답변 관련 API")
interface AnswerControllerDocs {

    @Operation(
        summary = "답변 작성",
        description = "오늘 배포 단위에 답변을 저장하고, 저장된 답변과 저장 후 활성 답변 수를 반환합니다.<br>" +
            "날짜는 서버가 요청을 받은 시점의 KST 날짜로 판정합니다. 질문 원문은 보내지 않고 질문 ID로 고릅니다. 질문은 오늘 SEMA의 기본 또는 대체 질문이어야 합니다.<br>" +
            "본문은 trim하지 않고 그대로 저장합니다. 확장 grapheme 기준 1~2,000자이고 공백만 있어도 유효합니다. 코드 포인트는 15,000개를 넘을 수 없습니다.<br>" +
            "판정 순서: DATE_CHANGED → SEMA_NOT_FOUND → SEMA_REPLACED → ANSWER_ALREADY_EXISTS → ANSWER_CONTENT_INVALID<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "✅ 답변 작성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류 (필드 누락, 빈 ID, 질문 원문 등 추가 필드)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "요청 형식 오류",
                            value = "{\"error\" : {\"code\" : \"INVALID_REQUEST\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "🚨 ACTIVE가 아닌 세션",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 mode 부족",
                            value = "{\"error\" : {\"code\" : \"SESSION_SCOPE_INSUFFICIENT\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "409",
            description = "🚨 오늘 배포 단위에 이미 답변 있음 (경쟁 요청 포함)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "답변 중복",
                            value = "{\"error\" : {\"code\" : \"ANSWER_ALREADY_EXISTS\", \"category\" : \"CONFLICT\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "422",
            description = "🚨 날짜 변경, SEMA 교체, 본문 규칙 위반",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "날짜 변경",
                            value = "{\"error\" : {\"code\" : \"DATE_CHANGED\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\", " +
                                "\"recovery\" : {\"kind\" : \"REFRESH_TODAY\"}}}"
                        ),
                        ExampleObject(
                            name = "SEMA 교체",
                            value = "{\"error\" : {\"code\" : \"SEMA_REPLACED\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\", " +
                                "\"recovery\" : {\"kind\" : \"REFRESH_TODAY\"}}}"
                        ),
                        ExampleObject(
                            name = "본문 규칙 위반",
                            value = "{\"error\" : {\"code\" : \"ANSWER_CONTENT_INVALID\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "503",
            description = "🚨 오늘 날짜에 편성된 SEMA 없음 (운영상 발생하지 않는 것으로 전제)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "오늘 SEMA 없음",
                            value = "{\"error\" : {\"code\" : \"SEMA_NOT_FOUND\", \"category\" : \"MAINTENANCE\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun createAnswer(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: CreateAnswerRequest
    ): ResponseEntity<CreateAnswerResponse>

    @Operation(
        summary = "기록 목록 조회",
        description = "내 답변을 answerId 내림차순으로 최대 20개씩 반환합니다. 항목에 답변 본문은 없고, 본문은 기록 상세 조회로 받습니다.<br>" +
            "첫 요청은 cursor를 생략하고, 이후에는 직전 응답의 nextCursor를 그대로 보냅니다. 다음 페이지가 없으면 nextCursor는 null입니다.<br>" +
            "이어 보는 중에 새로 쓴 답변은 이 목록에 끼지 않고 새로고침하면 보입니다. 수정한 답변은 최신 내용으로, 지운 답변은 빠진 채로 나옵니다.<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 기록 목록 조회 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 cursor 형식 오류 (숫자가 아님, 1 미만, 빈 문자열). 첫 페이지부터 다시 조회",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "cursor 형식 오류",
                            value = "{\"error\" : {\"code\" : \"CURSOR_INVALID\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "🚨 ACTIVE가 아닌 세션",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 mode 부족",
                            value = "{\"error\" : {\"code\" : \"SESSION_SCOPE_INSUFFICIENT\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun getAnswers(
        @Auth authSession: AuthSession,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<AnswerPageResponse>

    @Operation(
        summary = "기록 상세 조회",
        description = "내 답변 하나를 답변한 질문, 답변 원문 전체와 함께 반환합니다. 원문은 저장한 그대로입니다.<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 기록 상세 조회 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류 (숫자가 아닌 answerId)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "요청 형식 오류",
                            value = "{\"error\" : {\"code\" : \"INVALID_REQUEST\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "🚨 ACTIVE가 아닌 세션",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 mode 부족",
                            value = "{\"error\" : {\"code\" : \"SESSION_SCOPE_INSUFFICIENT\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "🚨 답변 없음, 삭제, 비소유 (구분하지 않음)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "답변 없음",
                            value = "{\"error\" : {\"code\" : \"ANSWER_NOT_FOUND\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun getAnswer(
        @Auth authSession: AuthSession,
        @PathVariable answerId: Long
    ): ResponseEntity<AnswerDetailResponse>

    @Operation(
        summary = "답변 수정",
        description = "답변 본문을 바꿉니다. 과거 날짜 답변도 수정할 수 있습니다.<br>" +
            "질문, SEMA, createdAt, createdDateKst는 유지하고 updatedAt을 갱신하며 isEdited를 true로 둡니다.<br>" +
            "본문 규칙은 답변 작성과 같습니다. 날짜, SEMA 검증은 하지 않습니다.<br>" +
            "판정 순서: ANSWER_NOT_FOUND → ANSWER_CONTENT_INVALID<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 답변 수정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류 (숫자가 아닌 answerId 포함)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "요청 형식 오류",
                            value = "{\"error\" : {\"code\" : \"INVALID_REQUEST\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "🚨 ACTIVE가 아닌 세션",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 mode 부족",
                            value = "{\"error\" : {\"code\" : \"SESSION_SCOPE_INSUFFICIENT\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "🚨 답변 없음, 삭제, 비소유 (구분하지 않음)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "답변 없음",
                            value = "{\"error\" : {\"code\" : \"ANSWER_NOT_FOUND\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "422",
            description = "🚨 본문 규칙 위반",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "본문 규칙 위반",
                            value = "{\"error\" : {\"code\" : \"ANSWER_CONTENT_INVALID\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun updateAnswer(
        @Auth authSession: AuthSession,
        @PathVariable answerId: Long,
        @Valid @RequestBody request: UpdateAnswerRequest
    ): ResponseEntity<UpdateAnswerResponse>

    @Operation(
        summary = "답변 삭제",
        description = "답변 하나를 삭제하고 삭제 후 활성 답변 수를 반환합니다. 되돌리기는 없습니다.<br>" +
            "오늘 답변을 지웠고 아직 같은 KST 날짜면 다시 작성할 수 있습니다.<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 답변 삭제 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류 (숫자가 아닌 answerId)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "요청 형식 오류",
                            value = "{\"error\" : {\"code\" : \"INVALID_REQUEST\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "🚨 ACTIVE가 아닌 세션",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 mode 부족",
                            value = "{\"error\" : {\"code\" : \"SESSION_SCOPE_INSUFFICIENT\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "🚨 답변 없음, 이미 삭제, 비소유 (구분하지 않음)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "답변 없음",
                            value = "{\"error\" : {\"code\" : \"ANSWER_NOT_FOUND\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun deleteAnswer(
        @Auth authSession: AuthSession,
        @PathVariable answerId: Long
    ): ResponseEntity<DeleteAnswerResponse>
}
