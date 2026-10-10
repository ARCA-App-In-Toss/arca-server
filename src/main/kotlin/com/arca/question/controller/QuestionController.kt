package com.arca.question.controller

import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.global.annotation.AllowedSessionModes
import com.arca.question.dto.response.TodayQuestionResponse
import com.arca.question.service.QuestionService
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/questions")
class QuestionController(
    private val questionService: QuestionService
) : QuestionControllerDocs {

    @AllowedSessionModes([ACTIVE])
    @GetMapping("/today")
    override fun getTodayQuestion(): ResponseEntity<TodayQuestionResponse> {
        val response = questionService.getTodayQuestion()
        return ResponseEntity.status(OK).body(response)
    }
}