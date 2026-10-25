package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import kr.byeongmin.watchtower.global.security.SessionMemberLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * global 패키지는 domain 을 참조하면 안되기 때문에 인터페이스로 간접적으로 참조하게 함
 * 인증 관련 기능이라 어쩔 수 없는 부분이 있음
 */
@Service
class SessionMemberLoaderImpl(
    private val memberAccessor: MemberAccessor
) : SessionMemberLoader {
    @Transactional(readOnly = true)
    override fun getMemberPrinciple(memberId: Long): MemberPrinciple {
        val member = memberAccessor.findMember(memberId)
            ?: throw BusinessException(AuthError.ACCOUNT_NOT_FOUND)

        member.ensureSignInAllowed()
        return MemberPrinciple(memberId, member.role.name)
    }
}
