# 테스트 코드 템플릿

## 통합 테스트

```kotlin
package com.arca.{domain}.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.arca.global.exception.domain.ExceptionCode.{ERROR_CODE}
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest

@IntegrationTest
class {Target}Test(
    private val target: {TargetClass},

    private val repository: {Repository}
) {
    @Nested
    inner class {기능}_테스트 {
        @BeforeEach
        fun setUp() {
            // 테스트 데이터 준비 (repository.save/saveAll)
        }

        @Test
        fun 정상_동작하면_성공한다() {
            //given

            //when
            val result = target.method(param)

            //then
            assertThat(result)...
        }

        @Test
        fun 존재하지_않으면_예외가_발생한다() {
            //given

            //when & then
            // 예외 타입과 exceptionCode를 함께 검증한다 (코드 누락 시 회귀 감지 불가)
            assertThatThrownBy { target.method(invalidParam) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", {ERROR_CODE})
        }
    }
}
```

## Fixture (엔티티 팩토리)

```kotlin
package com.arca.{domain}.fixture

import org.springframework.test.util.ReflectionTestUtils
import com.arca.{domain}.domain.{Entity}

object {Entity}Fixture {
    fun create{Entity}(
        {field}: {Type},
        {stateField}: {StateType}
    ): {Entity} {
        val entity = {Entity}.create(
            {field} = {field}
        )
        // 팩토리가 받지 않는 값(현재 인원, 생성 시각처럼 도메인 흐름이 바꾸는 값)만 리플렉션으로 채운다
        ReflectionTestUtils.setField(entity, "{stateField}", {stateField})

        return entity
    }
}
```
