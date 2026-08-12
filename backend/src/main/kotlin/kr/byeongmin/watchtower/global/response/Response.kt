package kr.byeongmin.watchtower.global.response

import java.time.LocalDateTime

interface Response {
    val success: Boolean
    val timestamp: LocalDateTime
}