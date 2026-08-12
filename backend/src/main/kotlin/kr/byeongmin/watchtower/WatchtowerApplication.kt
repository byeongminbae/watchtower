package kr.byeongmin.watchtower

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class WatchtowerApplication

fun main(args: Array<String>) {
    runApplication<WatchtowerApplication>(*args)
}
