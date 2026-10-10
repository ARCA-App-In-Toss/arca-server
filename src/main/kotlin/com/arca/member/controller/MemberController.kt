package com.arca.member.controller

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.dto.response.SessionResponse
import com.arca.global.annotation.AllowedSessionModes
import com.arca.global.annotation.Auth
import com.arca.member.dto.request.CreateMemberRequest
import com.arca.member.dto.request.UpdateNicknameRequest
import com.arca.member.dto.response.MemberProfileResponse
import com.arca.member.service.MemberService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/members")
class MemberController(
    private val memberService: MemberService
): MemberControllerDocs {

    @AllowedSessionModes([GUEST, ACTIVE])
    @PostMapping
    override fun createMember(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: CreateMemberRequest
    ): ResponseEntity<SessionResponse> {
        val response = memberService.createMember(authSession, request)
        return ResponseEntity.status(CREATED).body(response)
    }

    @AllowedSessionModes([GUEST, ACTIVE])
    @GetMapping("/me")
    override fun getMyMember(
        @Auth authSession: AuthSession
    ): ResponseEntity<MemberProfileResponse> {
        val response = memberService.getMyMember(authSession)
        return ResponseEntity.status(OK).body(response)
    }

    @AllowedSessionModes([ACTIVE])
    @PutMapping("/me/nickname")
    override fun updateNickname(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: UpdateNicknameRequest
    ): ResponseEntity<MemberProfileResponse> {
        val response = memberService.updateNickname(authSession, request)
        return ResponseEntity.status(OK).body(response)
    }
}