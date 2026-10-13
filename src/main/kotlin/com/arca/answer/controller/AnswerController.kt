package com.arca.answer.controller

import com.arca.answer.dto.request.CreateAnswerRequest
import com.arca.answer.dto.request.UpdateAnswerRequest
import com.arca.answer.dto.response.AnswerDetailResponse
import com.arca.answer.dto.response.AnswerPageResponse
import com.arca.answer.dto.response.CreateAnswerResponse
import com.arca.answer.dto.response.DeleteAnswerResponse
import com.arca.answer.dto.response.UpdateAnswerResponse
import com.arca.answer.service.AnswerService
import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.global.annotation.AllowedSessionModes
import com.arca.global.annotation.Auth
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/answers")
class AnswerController(
    private val answerService: AnswerService
) : AnswerControllerDocs {

    @AllowedSessionModes([ACTIVE])
    @PostMapping
    override fun createAnswer(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: CreateAnswerRequest
    ): ResponseEntity<CreateAnswerResponse> {
        val response = answerService.createAnswer(authSession, request)
        return ResponseEntity.status(CREATED).body(response)
    }

    @AllowedSessionModes([ACTIVE])
    @PutMapping("/{answerId}")
    override fun updateAnswer(
        @Auth authSession: AuthSession,
        @PathVariable answerId: Long,
        @Valid @RequestBody request: UpdateAnswerRequest
    ): ResponseEntity<UpdateAnswerResponse> {
        val response = answerService.updateAnswer(authSession, answerId, request)
        return ResponseEntity.status(OK).body(response)
    }

    @AllowedSessionModes([ACTIVE])
    @DeleteMapping("/{answerId}")
    override fun deleteAnswer(
        @Auth authSession: AuthSession,
        @PathVariable answerId: Long
    ): ResponseEntity<DeleteAnswerResponse> {
        val response = answerService.deleteAnswer(authSession, answerId)
        return ResponseEntity.status(OK).body(response)
    }

    @AllowedSessionModes([ACTIVE])
    @GetMapping
    override fun getAnswers(
        @Auth authSession: AuthSession,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<AnswerPageResponse> {
        val response = answerService.getAnswers(authSession, cursor)
        return ResponseEntity.status(OK).body(response)
    }

    @AllowedSessionModes([ACTIVE])
    @GetMapping("/{answerId}")
    override fun getAnswer(
        @Auth authSession: AuthSession,
        @PathVariable answerId: Long
    ): ResponseEntity<AnswerDetailResponse> {
        val response = answerService.getAnswer(authSession, answerId)
        return ResponseEntity.status(OK).body(response)
    }
}