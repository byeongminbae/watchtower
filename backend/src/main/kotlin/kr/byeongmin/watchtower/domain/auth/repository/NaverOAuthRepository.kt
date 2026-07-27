package kr.byeongmin.watchtower.domain.auth.repository

import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import org.springframework.data.jpa.repository.JpaRepository

interface NaverOAuthRepository : JpaRepository<NaverOAuth, Long> {
    fun existsByProviderId(providerId: String): Boolean
    fun findByProviderId(providerId: String): NaverOAuth
}