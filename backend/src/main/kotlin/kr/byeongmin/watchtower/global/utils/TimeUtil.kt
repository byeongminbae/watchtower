package kr.byeongmin.watchtower.global.utils

import java.time.LocalDateTime
import java.time.ZoneId

object TimeUtil {
    fun entityTime(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    fun debuggingTime(): LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}