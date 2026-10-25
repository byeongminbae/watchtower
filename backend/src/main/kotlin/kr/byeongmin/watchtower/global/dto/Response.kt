package kr.byeongmin.watchtower.global.dto

import java.time.LocalDateTime

/**
 * @property [isSuccess] 프론트 요청이 성공하였는지에 대한 여부
 * @property [timestamp] 프론트 엔지니어만 볼 시간이라 UTC가 아닌 KST 를 기준으로 함
 */
interface Response {
    val isSuccess: Boolean
    val timestamp: LocalDateTime
}