package kr.byeongmin.watchtower.global.error

interface Error {
    val statusCode: String
    val message: String
}