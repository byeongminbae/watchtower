package kr.byeongmin.watchtower.global.dto

import java.time.LocalDateTime

/**
 * @property [isSuccess] 프론트 요청이 성공하였는지에 대한 여부
 * @property [timestamp] 응답을 만든 시각(UTC)
 */
interface Response {
    val isSuccess: Boolean
    val timestamp: LocalDateTime
}