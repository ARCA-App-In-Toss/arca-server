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
        restApiException: RestApiException,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Business exception. requestId={}, code={}", requestId, restApiException.exceptionCode)

        return makeExceptionResponse(
            exceptionCode = restApiException.exceptionCode,
            requestId = requestId,
            exceptionRecovery = restApiException.exceptionRecovery,
            retryAfterSeconds = restApiException.retryAfterSeconds,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidException(
        methodArgumentNotValidException: MethodArgumentNotValidException,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String,
    ): ResponseEntity<ErrorResponse> {
        val fields = methodArgumentNotValidException.bindingResult.fieldErrors.joinToString(", ") { it.field }
        log.warn("Request validation failed. requestId={}, fields={}", requestId, fields)

        return makeExceptionResponse(INVALID_REQUEST, requestId)
    }

    @ExceptionHandler(
        HandlerMethodValidationException::class,
        ServletRequestBindingException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleInvalidParameterException(
        exception: Exception,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Request parameter invalid. requestId={}, type={}", requestId, exception.javaClass.simpleName)

        return makeExceptionResponse(INVALID_REQUEST, requestId)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        httpMessageNotReadableException: HttpMessageNotReadableException,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String,
    ): ResponseEntity<ErrorResponse> {
        val causeType = httpMessageNotReadableException.cause?.javaClass?.simpleName
        log.warn("Request body unreadable. requestId={}, cause={}", requestId, causeType)

        return makeExceptionResponse(INVALID_REQUEST, requestId)
    }

    @ExceptionHandler(
        NoResourceFoundException::class,
        HttpRequestMethodNotSupportedException::class,
        HttpMediaTypeNotSupportedException::class,
        HttpMediaTypeNotAcceptableException::class,
    )
    fun handleUnsupportedRequestException(
        exception: Exception,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Request unsupported. requestId={}, type={}", requestId, exception.javaClass.simpleName)

        return makeExceptionResponse(INVALID_REQUEST, requestId)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        @RequestAttribute(REQUEST_ID_ATTRIBUTE) requestId: String,
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected exception. requestId={}", requestId, exception)

        return makeExceptionResponse(INTERNAL_ERROR, requestId)
    }

    private fun makeExceptionResponse(
        exceptionCode: ExceptionCode,
        requestId: String,
        exceptionRecovery: ExceptionRecovery? = null,
        retryAfterSeconds: Int? = null,
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse.of(
            exceptionCode = exceptionCode,
            requestId = requestId,
            exceptionRecovery = exceptionRecovery,
            retryAfterSeconds = retryAfterSeconds,
        )

        val bodyBuilder = ResponseEntity.status(exceptionCode.httpStatus).contentType(APPLICATION_JSON)
        retryAfterSeconds?.let { bodyBuilder.header(RETRY_AFTER, it.toString()) }

        return bodyBuilder.body(response)
    }

    companion object {
        private val log = LogManager.getLogger(GlobalExceptionHandler::class.java)
    }
}
