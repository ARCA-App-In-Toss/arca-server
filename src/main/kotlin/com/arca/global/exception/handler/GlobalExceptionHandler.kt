package com.arca.global.exception.handler

import com.arca.global.exception.domain.ExceptionCode
import com.arca.global.exception.domain.ExceptionCode.INTERNAL_ERROR
import com.arca.global.exception.domain.ExceptionCode.INVALID_REQUEST
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import com.arca.global.exception.dto.response.ErrorResponse
import com.arca.global.infra.RequestIdFilter.Companion.REQUEST_ID_ATTRIBUTE
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException::class)
    fun handleRestApiException(
        e: RestApiException,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String
    ): ResponseEntity<ErrorResponse> {
        log.warn("Business exception occurred. requestId = {}, code = {}", requestId, e.exceptionCode)

        return makeExceptionResponse(
            exceptionCode = e.exceptionCode,
            requestId = requestId,
            exceptionRecovery = e.exceptionRecovery,
            retryAfterSeconds = e.retryAfterSeconds
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidationException(
        e: MethodArgumentNotValidException,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String
    ): ResponseEntity<ErrorResponse> {
        val fields = e.bindingResult.fieldErrors.joinToString(", ") { it.field }
        log.warn("Bean validation exception occurred. requestId = {}, fields = {}", requestId, fields)

        return makeExceptionResponse(
            exceptionCode = INVALID_REQUEST,
            requestId = requestId
        )
    }

    @ExceptionHandler(
        HandlerMethodValidationException::class,
        ServletRequestBindingException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun handleInvalidParameterException(
        e: Exception,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String
    ): ResponseEntity<ErrorResponse> {
        log.warn("Invalid parameter exception occurred. requestId = {}, type = {}", requestId, e.javaClass.simpleName)

        return makeExceptionResponse(
            exceptionCode = INVALID_REQUEST,
            requestId = requestId
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String
    ): ResponseEntity<ErrorResponse> {
        val causeType = e.cause?.javaClass?.simpleName
        log.warn("Http message not readable exception occurred. requestId = {}, cause = {}", requestId, causeType)

        return makeExceptionResponse(
            exceptionCode = INVALID_REQUEST,
            requestId = requestId
        )
    }

    @ExceptionHandler(
        NoResourceFoundException::class,
        HttpRequestMethodNotSupportedException::class,
        HttpMediaTypeNotSupportedException::class,
        HttpMediaTypeNotAcceptableException::class
    )
    fun handleUnsupportedRequestException(
        e: Exception,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String
    ): ResponseEntity<ErrorResponse> {
        log.warn("Unsupported request exception occurred. requestId = {}, type = {}", requestId, e.javaClass.simpleName)

        return makeExceptionResponse(
            exceptionCode = INVALID_REQUEST,
            requestId = requestId
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected exception occurred. requestId = {}", requestId, e)

        return makeExceptionResponse(
            exceptionCode = INTERNAL_ERROR,
            requestId = requestId
        )
    }

    private fun makeExceptionResponse(
        exceptionCode: ExceptionCode,
        requestId: String,
        exceptionRecovery: ExceptionRecovery? = null,
        retryAfterSeconds: Int? = null
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse.of(
            exceptionCode = exceptionCode,
            requestId = requestId,
            exceptionRecovery = exceptionRecovery,
            retryAfterSeconds = retryAfterSeconds
        )

        val bodyBuilder = ResponseEntity.status(exceptionCode.httpStatus).contentType(APPLICATION_JSON)
        retryAfterSeconds?.let { bodyBuilder.header(RETRY_AFTER, it.toString()) }

        return bodyBuilder.body(response)
    }

    companion object {
        private val log = LogManager.getLogger(GlobalExceptionHandler::class.java)
    }
}
