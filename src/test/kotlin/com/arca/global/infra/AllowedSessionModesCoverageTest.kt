package com.arca.global.infra

import com.arca.global.annotation.AllowedSessionModes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@IntegrationTest
class AllowedSessionModesCoverageTest(
    @param:Qualifier("requestMappingHandlerMapping")
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping
) {

    @Test
    fun v1_엔드포인트는_공개_목록을_빼면_모두_허용_mode를_선언한다() {
        //when
        val undeclaredHandlers = requestMappingHandlerMapping.handlerMethods
            .filter { (requestMappingInfo, handlerMethod) ->
                handlerMethod.beanType !in TEST_CONTROLLERS &&
                    requestMappingInfo.patternValues.any { it.startsWith(API_PREFIX) }
            }
            .filterNot { (_, handlerMethod) -> handlerMethod.hasMethodAnnotation(AllowedSessionModes::class.java) }
            .map { (_, handlerMethod) -> "${handlerMethod.beanType.simpleName}.${handlerMethod.method.name}" }
            .filterNot { it in PUBLIC_HANDLERS }

        //then
        assertThat(undeclaredHandlers).isEmpty()
    }

    companion object {
        private const val API_PREFIX = "/v1"
        private val TEST_CONTROLLERS = setOf<Class<*>>(TestController::class.java, AuthTestController::class.java)
        private val PUBLIC_HANDLERS = setOf("AuthSessionController.createSession")
    }
}
