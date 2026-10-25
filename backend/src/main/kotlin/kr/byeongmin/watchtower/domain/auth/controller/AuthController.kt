package kr.byeongmin.watchtower.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.domain.auth.dto.CsrfTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.dto.MemberSessionResponseDto
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.security.MemberOnly
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    /**
     * @param [request] 여기서는 쿠키에 접근하기 위해 사용
     * @param [response] 여기서는 스프링 시큐리티 응답 헤더와 쿠키를 처리하기 위해 사용
     */
    @Operation(summary = "네이버 OAuth URL 생성 및 반환")
    @GetMapping("/naver/url")
    fun getNaverSignInUrl(
        @Parameter(description = "로그인 요청을 확인하기 위해 세션에 저장할 csrf token 값")
        @RequestParam(value = "state") state: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): SuccessDataResponse<String> {
        return authService.getNaverSignInUrl(state, request, response)
    }

    /**
     * @param [state] 사용자가 우리측에서 진행한 바가 없는 요청인데 공격자가 네이버 로그인 url(getNaverSignInUrl)에 넘어가는 파라미터의
     *  의미를 파악하고 변조한 뒤 피해자에게 보낼 경우 피해자가 로그인 하더라도 우리측에선 피해자 브라우저의 세션에 저장된 csrf 토큰과 해당 요청의
     *  csrf 토큰이 일치하지 않기 때문에 거부할 수 있음
     */
    @Operation(summary = "네이버 로그인 성공 후 기존 회원은 로그인, 신규 회원은 가입하고 세션을 생성함")
    @GetMapping("/naver/callback")
    fun signInWithNaverCallback(
        @Parameter(description = "네이버 로그인 인증에 성공하면 반환받는 일회성 인증 코드, 네이버 토큰 발급에 사용")
        @RequestParam(value = "code") code: String,
        @Parameter(description = "네이버가 돌려준 로그인 요청 당시 세션에 저장한 csrf token 값")
        @RequestParam(value = "state") state: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): SuccessDataResponse<MemberSessionResponseDto> {
        return authService.signInWithNaverCallback(code, state, request, response)
    }

    @Operation(summary = "현재 로그인한 회원의 세션 조회")
    @GetMapping("/session")
    @MemberOnly
    fun getSession(
        @AuthenticationPrincipal principal: MemberPrinciple
    ): SuccessDataResponse<MemberSessionResponseDto> {
        return SuccessDataResponse(MemberSessionResponseDto(principal.memberId, principal.role))
    }

    /**
     * 기존 토큰이 없으면 스프링이 토큰을 생성해서 세션(없으면 같이 생성)에 저장함.
     *
     * (실행 흐름 1) 세션 A:
     *  CSRF 토큰
     *
     * [CsrfToken.token] 이후 검증 대상 요청에 넣을 토큰 값
     * [CsrfToken.getHeaderName] 토큰을 넣을 헤더 이름. 현재 X-CSRF-TOKEN
     */
    @Operation(summary = "변경 요청에 사용할 CSRF 토큰 조회")
    @GetMapping("/csrf")
    fun getCsrfToken(csrfToken: CsrfToken): SuccessDataResponse<CsrfTokenResponseDto> {
        return SuccessDataResponse(CsrfTokenResponseDto(csrfToken.token, csrfToken.headerName))
    }
}
