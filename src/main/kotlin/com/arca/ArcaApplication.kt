package com.arca

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class ArcaApplication

fun main(args: Array<String>) {
    runApplication<ArcaApplication>(*args)
}
