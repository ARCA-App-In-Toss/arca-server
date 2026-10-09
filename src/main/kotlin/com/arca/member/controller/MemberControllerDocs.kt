package com.arca.member.controller

import com.arca.auth.domain.AuthSession
import com.arca.auth.dto.response.SessionResponse
import com.arca.global.annotation.Auth
import com.arca.global.exception.dto.response.ErrorResponse
import com.arca.member.dto.request.CreateMemberRequest
import com.arca.member.dto.request.UpdateNicknameRequest
import com.arca.member.dto.response.MemberProfileResponse
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
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Member API", description = "회원 관련 API")
interface MemberControllerDocs {

    @Operation(
        summary = "회원 가입(약관 동의)",
        description = "필수 약관 동의를 기록하고 회원을 만듭니다. 기존 GUEST 토큰을 폐기하고 ACTIVE 토큰을 발급합니다.<br>" +
            "ACTIVE 세션의 요청은 403이 아니라 항상 409입니다.<br>" +
            "🔐 <strong>세션 필요 (GUEST)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "✅ 회원 가입 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류",
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
            responseCode = "401",
            description = "🚨 폐기된 GUEST 토큰 (응답 유실 뒤 재전송 등)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 재교환 필요",
                            value = "{\"error\" : {\"code\" : \"SESSION_RECOVERY_REQUIRED\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\", " +
                                "\"recovery\" : {\"kind\" : \"REESTABLISH_SESSION\", \"recoveryAllowed\" : true}}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "409",
            description = "🚨 이미 회원이 있음 (ACTIVE 세션, 가입한 키의 다른 GUEST 토큰, 경쟁 가입에서 짐)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "회원 중복",
                            value = "{\"error\" : {\"code\" : \"MEMBER_ALREADY_EXISTS\", \"category\" : \"CONFLICT\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "422",
            description = "🚨 정책 동의 검증 실패",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "정책 버전 변경",
                            value = "{\"error\" : {\"code\" : \"POLICY_VERSION_CHANGED\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\", " +
                                "\"recovery\" : {\"kind\" : \"REFRESH_POLICIES\", \"policies\" : [" +
                                "{\"policyId\" : \"terms-of-service\", \"version\" : \"1\", \"title\" : \"서비스 이용약관\", \"url\" : \"https://arca.invalid/policies/terms-of-service/1\", \"required\" : true}, " +
                                "{\"policyId\" : \"privacy-policy\", \"version\" : \"1\", \"title\" : \"개인정보처리방침\", \"url\" : \"https://arca.invalid/policies/privacy-policy/1\", \"required\" : true}" +
                                "]}}}"
                        ),
                        ExampleObject(
                            name = "필수 동의 누락",
                            value = "{\"error\" : {\"code\" : \"CONSENT_REQUIRED\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun createMember(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: CreateMemberRequest
    ): ResponseEntity<SessionResponse>

    @Operation(
        summary = "회원 정보 조회",
        description = "현재 회원 profile을 반환합니다. GUEST 세션은 403이 아니라 항상 404입니다.<br>" +
            "🔐 <strong>세션 필요 (ACTIVE, GUEST)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 회원 정보 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "🚨 회원 없음 (GUEST 세션 포함)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "회원 없음",
                            value = "{\"error\" : {\"code\" : \"MEMBER_NOT_FOUND\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun getMyMember(
        @Auth authSession: AuthSession
    ): ResponseEntity<MemberProfileResponse>

    @Operation(
        summary = "닉네임 설정, 해제",
        description = "닉네임을 설정하거나 null로 해제합니다. expectedRevision이 현재 revision과 같을 때만 바꿉니다.<br>" +
            "앞뒤 공백을 제거한 뒤 확장 grapheme 기준 2~12자여야 하고, 줄바꿈, 제어, 보이지 않는 문자는 거절합니다.<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 닉네임 변경 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류 (nickname 생략 포함)",
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
            description = "🚨 revision 불일치",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "revision 불일치",
                            value = "{\"error\" : {\"code\" : \"REVISION_CONFLICT\", \"category\" : \"CONFLICT\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "422",
            description = "🚨 닉네임 규칙 위반",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "닉네임 규칙 위반",
                            value = "{\"error\" : {\"code\" : \"NICKNAME_INVALID\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun updateNickname(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: UpdateNicknameRequest
    ): ResponseEntity<MemberProfileResponse>
}
