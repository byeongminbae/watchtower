package kr.byeongmin.watchtower.global.entity

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
abstract class Base {
    // SEQUENCE 전략 사용으로 인해 아이디는 각 엔티티에서 정의함

    @CreatedDate
    private val createdAt: LocalDateTime? = null

    @LastModifiedDate
    private val updatedAt: LocalDateTime? = null

    private var deletedAt: LocalDateTime? = null

    fun delete() {
        deletedAt = LocalDateTime.now()
    }
}