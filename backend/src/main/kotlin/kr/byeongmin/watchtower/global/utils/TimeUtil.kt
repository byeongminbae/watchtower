package kr.byeongmin.watchtower.global.utils

import java.time.LocalDateTime
import java.time.ZoneId

object TimeUtil {
    fun currentUTCTime(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    fun currentKSTTime(): LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}