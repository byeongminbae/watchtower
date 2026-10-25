package kr.byeongmin.watchtower.global.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("isAuthenticated()")
annotation class MemberOnly

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
annotation class AdminOnly

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("isAuthenticated() and #memberId == principal.memberId")
annotation class OwnerOnly

/** 자체적으로 인증을 요구하지는 않지만 파라미터에 memberId 를 주입해줌 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@AuthenticationPrincipal(
    expression = "memberId",
    errorOnInvalidType = true
)
annotation class MemberId
