package com.arca.global.infra

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestConstructor.AutowireMode.ALL
import org.springframework.transaction.annotation.Transactional
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS


@SpringBootTest
@Transactional
@TestConstructor(autowireMode = ALL)
@Target(CLASS)
@Retention(RUNTIME)
annotation class IntegrationTest
