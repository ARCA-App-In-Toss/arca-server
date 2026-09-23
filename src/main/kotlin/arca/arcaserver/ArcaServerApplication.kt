package arca.arcaserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ArcaServerApplication

fun main(args: Array<String>) {
    runApplication<ArcaServerApplication>(*args)
}
