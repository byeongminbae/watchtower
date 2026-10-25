package kr.byeongmin.watchtower.domain.auth.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.auth.dto.MemberSessionResponseDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.external.naver.client.NaverAuthClient
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class AuthService(
    private val naverAuthClient: NaverAuthClient,
    private val memberAccessor: MemberAccessor,
    private val naverOAuthAccessor: NaverOAuthAccessor,
    private val planAccessor: PlanAccessor,
    private val timeProvider: TimeProvider,
    private val authorizationRequestRepository: AuthorizationRequestRepository<OAuth2AuthorizationRequest>,
    private val sessionAuthenticationStrategy: SessionAuthenticationStrategy,
    private val securityContextRepository: SecurityContextRepository,
    private val transactionTemplate: TransactionTemplate
) {
    fun getNaverSignInUrl(
        state: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): SuccessDataResponse<String> {
        val authorizationRequest = naverAuthClient.getAuthorizationRequest(state)
        /**
         * (실행흐름 2) 세션 A:
         * CSRF 토큰
         * OAuth 로그인 시도 정보: state, 인가 URL 등
         */
        authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response)
        return SuccessDataResponse(authorizationRequest.authorizationRequestUri)
    }

    fun signInWithNaverCallback(
        code: String,
        state: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): SuccessDataResponse<MemberSessionResponseDto> {
        /**
         * 로그인 URL 조회 때 세션에 보관한 state와 콜백 요청의 state를 비교함
         *
         * 일치하면 state와 인가 URL 등이 들어 있는 OAuth2AuthorizationRequest 객체를 세션 속성에서 삭제함
         *
         * 이 로그인 시도의 임시 기록을 소비해서 다음 콜백에서 같은 state를 다시 검증하는 데 쓰지 못하게 함
         *
         * (실행흐름 3) 세션 A:
         *   API CSRF 토큰
         *
         * @exception [BusinessException] 로그인 시도가 없거나 state 가 유효하지 않은 경우
         */
        authorizationRequestRepository.removeAuthorizationRequest(request, response)
            ?: throw BusinessException(AuthError.INVALID_OAUTH_STATE)

        /**
         * 이 트랜젝션은 즉시 반영되어야 해서 이렇게 처리함(@Transactional 로 처리하면 메서드 끝에서 커밋됨)
         */
        val principal = transactionTemplate.execute {
            signInWithNaver(code, state)
        }.ifNullThrow()

        val usernamePasswordAuthenticationToken = principal.to()

        /**
         * [kr.byeongmin.watchtower.global.security.SecurityConfig] 에 등록된 빈에 따라 작동
         *
         * 1. 해당 구현체에 따르면 세션 고정 고오격 방지를 위해 기존 세션id만 변경함(A-> B)
         * 2. HttpSessionCsrfTokenRepository 를 CsrfAuthenticationStrategy 가 인자로 받으면서 세션에 존재하는 csrf 토큰을 삭제함
         */
        sessionAuthenticationStrategy.onAuthentication(usernamePasswordAuthenticationToken, request, response)

        saveSecurityContext(usernamePasswordAuthenticationToken, request, response)
        return SuccessDataResponse(MemberSessionResponseDto(principal.memberId, principal.role))
    }

    private fun saveSecurityContext(
        usernamePasswordAuthenticationToken: UsernamePasswordAuthenticationToken,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = usernamePasswordAuthenticationToken
        // 이 요청에서 인증 정보가 유효하도록 스레드 로컬에 올림
        SecurityContextHolder.setContext(securityContext)
        // 다음 요청 때 브라우저가 JSESSIONID 쿠키를 보내면 필터가 세션에서 컨텍스트를 꺼내서 쓰레드 로컬에 복원함
        securityContextRepository.saveContext(securityContext, request, response)
    }

    private fun signInWithNaver(code: String, state: String): MemberPrinciple {
        val naverTokenResponseDto = naverAuthClient.fetchToken(code, state)
        val naverProfileResponseDto = naverAuthClient.fetchProfile(naverTokenResponseDto.accessToken)
        val providerId = naverProfileResponseDto.response.providerId

        val naverOAuth = naverOAuthAccessor.findByProviderId(providerId)
        val member = naverOAuth?.member ?: naverSignUp(
            naverProfileResponseDto = naverProfileResponseDto,
            naverTokenResponseDto = naverTokenResponseDto
        )

        member.ensureSignInAllowed()
        member.completeSignIn(timeProvider.now())
        return MemberPrinciple(member.id.ifNullThrow(), member.role.name)
    }

    private fun naverSignUp(
        naverProfileResponseDto: NaverProfileResponseDto,
        naverTokenResponseDto: NaverTokenResponseDto
    ): Member {
        val freePlan = planAccessor.findByType(PlanType.FREE)
            ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)

        val member = memberAccessor.save(
            Member.from(
                naverProfileResponseDto = naverProfileResponseDto,
                now = timeProvider.now(),
                plan = freePlan
            )
        )
        naverOAuthAccessor.save(
            NaverOAuth.from(
                member = member,
                naverProfileResponseDto = naverProfileResponseDto,
                naverTokenResponseDto = naverTokenResponseDto
            )
        )

        return member
    }
}
