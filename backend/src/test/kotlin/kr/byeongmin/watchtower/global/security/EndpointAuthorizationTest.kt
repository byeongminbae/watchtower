package kr.byeongmin.watchtower.global.security

import kr.byeongmin.watchtower.domain.admin.controller.AdminController
import kr.byeongmin.watchtower.domain.admin.controller.StatsController
import kr.byeongmin.watchtower.domain.admin.service.AdminService
import kr.byeongmin.watchtower.domain.admin.service.StatsService
import kr.byeongmin.watchtower.domain.auth.controller.AuthController
import kr.byeongmin.watchtower.domain.auth.dto.MemberSessionResponseDto
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.domain.member.controller.MemberController
import kr.byeongmin.watchtower.domain.member.service.MemberService
import kr.byeongmin.watchtower.domain.notification.controller.NotificationChannelController
import kr.byeongmin.watchtower.domain.notification.service.NotificationChannelService
import kr.byeongmin.watchtower.domain.tower.controller.TowerController
import kr.byeongmin.watchtower.domain.tower.service.HotTowerService
import kr.byeongmin.watchtower.domain.tower.service.TowerService
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.aop.framework.ProxyFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertFailsWith

class EndpointAuthorizationTest {
    private val adminService = mock<AdminService>()
    private val statsService = mock<StatsService>()
    private val memberService = mock<MemberService>()
    private val channelService = mock<NotificationChannelService>()
    private val towerService = mock<TowerService>()
    private val hotTowerService = mock<HotTowerService>()
    private val authService = mock<AuthService>()
    private val request = MockHttpServletRequest()
    private val response = MockHttpServletResponse()
    private val adminController = secure(AdminController(adminService))
    private val statsController = secure(StatsController(statsService))
    private val memberController = secure(MemberController(memberService))
    private val notificationChannelController = secure(NotificationChannelController(channelService))
    private val towerController = secure(TowerController(towerService, hotTowerService))
    private val authController = secure(AuthController(authService))

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> secure(controller: T): T {
        val factory = ProxyFactory(controller)
        factory.isProxyTargetClass = true
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize())
        return factory.proxy as T
    }

    private fun authenticate(role: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            MemberPrinciple(21L, role),
            null,
            listOf(SimpleGrantedAuthority("ROLE_$role"))
        )
    }

    @Test
    fun `관리자는 관리자 전용 API를 사용할 수 있음`() {
        /** given */
        authenticate("ADMIN")

        /** when */
        adminController.getMembers(OffsetPaginationRequestDto(), BaseSortField.ID)
        statsController.getTowerStats()

        /** then */
        verify(adminService).getMembers(OffsetPaginationRequestDto(), BaseSortField.ID)
        verify(statsService).getTowerStats()
    }

    @Test
    fun `일반 회원은 관리자 전용 API를 사용할 수 없음`() {
        /** given */
        authenticate("NORMAL")

        /** when */
        assertFailsWith<AccessDeniedException> {
            adminController.getMembers(OffsetPaginationRequestDto(), BaseSortField.ID)
        }
        assertFailsWith<AccessDeniedException> { statsController.getTowerStats() }

        /** then */
        verifyNoInteractions(adminService, statsService)
    }

    @Test
    fun `로그인하지 않으면 관리자 전용 API를 사용할 수 없음`() {
        /** given */
        SecurityContextHolder.clearContext()

        /** when */
        assertFailsWith<AuthenticationCredentialsNotFoundException> {
            adminController.getMembers(OffsetPaginationRequestDto(), BaseSortField.ID)
        }
        assertFailsWith<AuthenticationCredentialsNotFoundException> { statsController.getTowerStats() }

        /** then */
        verifyNoInteractions(adminService, statsService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NORMAL", "ADMIN"])
    fun `회원용 API에서는 권한과 관계없이 다른 회원의 계정에 접근할 수 없음`(role: String) {
        /** given */
        authenticate(role)

        /** when */
        assertFailsWith<AccessDeniedException> { memberController.getMember(99L) }

        /** then */
        verifyNoInteractions(memberService)
    }

    @Test
    fun `로그인한 회원은 자신의 프로필을 조회할 수 있음`() {
        /** given */
        authenticate("NORMAL")

        /** when */
        memberController.getMember(21L)

        /** then */
        verify(memberService).getMember(21L)
    }

    @Test
    fun `로그인한 회원은 알림 채널과 타워 및 세션을 조회할 수 있음`() {
        /** given */
        authenticate("NORMAL")

        /** when */
        notificationChannelController.getChannel(21L, 1L)
        towerController.getTower(21L, 7L)
        authController.getSession(MemberPrinciple(21L, "NORMAL"))

        /** then */
        verify(channelService).getChannel(21L, 1L)
        verify(towerService).getTower(21L, 7L)
    }

    @Test
    fun `로그인하지 않으면 회원용 API를 사용할 수 없음`() {
        /** given */
        SecurityContextHolder.clearContext()

        /** when */
        assertFailsWith<AuthenticationCredentialsNotFoundException> { memberController.getMember(21L) }
        assertFailsWith<AuthenticationCredentialsNotFoundException> { notificationChannelController.getChannel(21L, 1L) }
        assertFailsWith<AuthenticationCredentialsNotFoundException> { towerController.getTower(21L, 7L) }
        assertFailsWith<AuthenticationCredentialsNotFoundException> {
            authController.getSession(MemberPrinciple(21L, "NORMAL"))
        }

        /** then */
        verifyNoInteractions(memberService, channelService, towerService, authService)
    }

    @Test
    fun `인기 타워 조회와 네이버 로그인은 로그인하지 않아도 사용할 수 있음`() {
        /** given */
        SecurityContextHolder.clearContext()
        whenever(authService.getNaverSignInUrl("state", request, response))
            .thenReturn(SuccessDataResponse("https://nid.naver.com/oauth2.0/authorize"))
        whenever(authService.signInWithNaverCallback("code", "state", request, response))
            .thenReturn(SuccessDataResponse(MemberSessionResponseDto(21L, "NORMAL")))

        /** when */
        towerController.getTrendingTowers()
        authController.getNaverSignInUrl("state", request, response)
        authController.signInWithNaverCallback("code", "state", request, response)

        /** then */
        verify(hotTowerService).getTrendingTowers()
        verify(authService).getNaverSignInUrl("state", request, response)
        verify(authService).signInWithNaverCallback("code", "state", request, response)
    }
}
