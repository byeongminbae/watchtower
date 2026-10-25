package kr.byeongmin.watchtower.global.config

import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 테스트를 더 촘촘하게 짜기 위해 프로젝트 전체에서 공통적으로 사용되는 시각 공급자 클래스
 *
 * 테스트를 제외하고는 new 로 생성하지 말고 이 빈을 주입받아 사용해야한다
 */
@Component
class TimeProvider {
    fun now(): LocalDateTime {
        return LocalDateTime.now(ZoneId.of("UTC"))
    }
}
