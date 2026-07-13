package kr.byeongmin.watchtower

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WatchtowerApplication

fun main(args: Array<String>) {
    runApplication<WatchtowerApplication>(*args)
}
