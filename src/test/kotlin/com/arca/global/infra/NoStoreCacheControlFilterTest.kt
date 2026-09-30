package com.arca.global.infra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@IntegrationTest
@AutoConfigureMockMvc
class NoStoreCacheControlFilterTest(
    private val mockMvc: MockMvc,
) {

    @Test
    fun 성공_응답에_no_store가_붙는다() {
        //when
        val response = mockMvc.get(PATH).andReturn().response

        //then
        assertThat(response.status).isEqualTo(200)
        assertThat(response.getHeader(CACHE_CONTROL)).isEqualTo(NO_STORE)
    }

    @Test
    fun 요청_형식_오류_응답에_no_store가_붙는다() {
        //when
        val response = mockMvc.post(PATH) {
            contentType = APPLICATION_JSON
            content = "{"
        }.andReturn().response

        //then
        assertThat(response.status).isEqualTo(400)
        assertThat(response.getHeader(CACHE_CONTROL)).isEqualTo(NO_STORE)
    }

    @Test
    fun 없는_경로의_오류_응답에_no_store가_붙는다() {
        //when
        val response = mockMvc.get(UNKNOWN_PATH).andReturn().response

        //then
        assertThat(response.status).isEqualTo(404)
        assertThat(response.getHeader(CACHE_CONTROL)).isEqualTo(NO_STORE)
    }

    companion object {
        private const val PATH = "/v1/test"
        private const val UNKNOWN_PATH = "/v1/unknown"
        private const val NO_STORE = "no-store"
    }
}
