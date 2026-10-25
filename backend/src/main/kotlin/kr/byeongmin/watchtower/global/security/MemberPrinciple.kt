package kr.byeongmin.watchtower.global.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.io.Serializable

class MemberPrinciple(
    val memberId: Long,
    val role: String
) : Serializable {
    fun to(): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken.authenticated(
            this,
            null,
            listOf(SimpleGrantedAuthority("ROLE_$role"))
        )
    }
}
