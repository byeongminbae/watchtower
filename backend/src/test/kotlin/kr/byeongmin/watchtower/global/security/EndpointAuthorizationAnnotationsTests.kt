package kr.byeongmin.watchtower.global.security

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringJUnitConfig(EndpointAuthorizationAnnotationsTests.TestConfig::class)
class EndpointAuthorizationAnnotationsTests @Autowired constructor(
    private val securedEndpoint: SecuredEndpoint,
) {
    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `owner can access own member resource`() {
        authenticate(7, "USER")

        assertEquals(7, securedEndpoint.getMember(7))
    }

    @Test
    fun `owner cannot access another member resource`() {
        authenticate(7, "USER")

        assertFailsWith<AccessDeniedException> {
            securedEndpoint.getMember(8)
        }
    }

    @Test
    fun `anonymous user cannot access member resource`() {
        assertFailsWith<AuthenticationCredentialsNotFoundException> {
            securedEndpoint.getMember(7)
        }
    }

    @Test
    fun `admin annotation accepts only admin role`() {
        authenticate(1, "USER")
        assertFailsWith<AccessDeniedException> {
            securedEndpoint.getAdminStats()
        }

        authenticate(2, "ADMIN")
        assertEquals("stats", securedEndpoint.getAdminStats())
    }

    private fun authenticate(memberId: Long, role: String) {
        val principal = WatchtowerPrincipal(memberId, role)
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_$role")),
        )
    }

    open class SecuredEndpoint {
        @OwnerOnly
        open fun getMember(memberId: Long): Long = memberId

        @AdminOnly
        open fun getAdminStats(): String = "stats"
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    class TestConfig {
        @Bean
        fun securedEndpoint(): SecuredEndpoint = SecuredEndpoint()
    }
}
