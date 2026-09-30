package com.arca.global.config

import com.arca.global.infra.IntegrationTest
import com.arca.global.infra.TestController.Companion.CREATED_AT
import com.arca.global.infra.TestController.TestRequest
import com.arca.global.infra.TestController.TestResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

@IntegrationTest
@AutoConfigureMockMvc
class JacksonConfigTest(
    private val jsonMapper: JsonMapper,

    private val mockMvc: MockMvc,
) {

    @Nested
    inner class 요청을_역직렬화할_때 {

        @Test
        fun 계약에_있는_필드만_있으면_성공한다() {
            //given
            val json = """{"name": "아르카"}"""

            //when
            val request = jsonMapper.readValue(json, TestRequest::class.java)

            //then
            assertThat(request.name).isEqualTo("아르카")
        }

        @Test
        fun 알_수_없는_필드가_있으면_실패한다() {
            //given
            val json = """{"name": "아르카", "unknown": 1}"""

            //when & then
            assertThatThrownBy { jsonMapper.readValue(json, TestRequest::class.java) }
                .isInstanceOf(UnrecognizedPropertyException::class.java)
        }

        @Test
        fun 알_수_없는_필드가_있는_요청은_400으로_거절된다() {
            //given
            val json = """{"name": "아르카", "unknown": 1}"""

            //when
            val response = mockMvc.post(PATH) {
                contentType = APPLICATION_JSON
                content = json
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(400)
        }
    }

    @Nested
    inner class 응답을_직렬화할_때 {

        @Test
        fun null인_필드를_생략하지_않고_내보낸다() {
            //given
            val response = TestResponse(
                name = null,
                createdAt = Instant.parse(CREATED_AT),
            )

            //when
            val json = jsonMapper.writeValueAsString(response)

            //then
            assertThat(json).contains("\"name\":null")
        }

        @Test
        fun Instant를_Z로_끝나는_UTC_문자열로_내보낸다() {
            //given
            val response = TestResponse(
                name = null,
                createdAt = Instant.parse(CREATED_AT),
            )

            //when
            val json = jsonMapper.writeValueAsString(response)

            //then
            assertThat(json).contains("\"createdAt\":\"$CREATED_AT\"")
        }

        @Test
        fun HTTP_응답_본문에도_같은_규칙이_적용된다() {
            //when
            val body = mockMvc.get(PATH).andReturn().response.contentAsString

            //then
            assertThat(body).isEqualTo("""{"name":null,"createdAt":"$CREATED_AT"}""")
        }
    }

    companion object {
        private const val PATH = "/v1/test"
    }
}
