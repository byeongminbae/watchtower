package kr.byeongmin.watchtower.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.domain.auth.dto.CsrfTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.dto.MemberSessionResponseDto
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.security.MemberOnly
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    @Operation(summary = "현재 로그인한 회원의 세션 조회")
    @GetMapping("/session")
    @MemberOnly
    fun getSession(
        @AuthenticationPrincipal principal: MemberPrinciple
    ): SuccessDataResponse<MemberSessionResponseDto> {
        return SuccessDataResponse(MemberSessionResponseDto(principal.memberId, principal.role))
    }

    /**
     * @param [csrfToken] 스프링 시큐리티 필터가 전달해주는 토큰 값
     */
    @Operation(
        summary = "프론트가 POST, PUT, DELETE 요청마다 X-CSRF-TOKEN 헤더에 담아야 하는 CSRF 토큰 발행",
        description = "기존 토큰이 없으면 스프링이 토큰을 생성해서 세션(없으면 같이 생성)에 저장함"
    )
    @GetMapping("/csrf")
    fun getCsrfToken(csrfToken: CsrfToken): SuccessDataResponse<CsrfTokenResponseDto> {
        return SuccessDataResponse(CsrfTokenResponseDto(csrfToken.token, csrfToken.headerName))
    }
}
