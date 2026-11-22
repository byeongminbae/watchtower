package kr.byeongmin.watchtower.global.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import java.io.Serializable

class MemberPrinciple(
    val memberId: Long,
    val role: String
) : OAuth2User, Serializable {
    override fun getName(): String {
        return memberId.toString()
    }

    override fun getAttributes(): Map<String, Any> {
        return emptyMap()
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_$role"))
    }

    fun to(): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken.authenticated(this, null, authorities)
    }
}
