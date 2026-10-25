package kr.byeongmin.watchtower.global.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 생성, 수정 시각은 JPA가 기록하고 삭제 시각은 [delete]로 설정하는 공통 슈퍼클래스
 *
 * [HistoryBase] 에 해당하지 않는 대부분의 엔티티는 이 클래스를 상속받아야 함
 *
 * 어드민 같은 예외도 있어서 논리 삭제된 행을 조회에서 자동으로 제외하지는 않음
 *
 * querydsl 같은걸로 업데이트하거나 삭제 할 경우 [updatedAt] 이 자동으로 업데이트 되지 않음
 *
 * @property [id] 엔티티마다 사용하는 DB 시퀀스가 다르므로 하위 엔티티에서 @Id와 생성 전략을 정의해야함
 * @property [createdAt] UTC 생성 시각
 * @property [updatedAt] 마지막 수정 시각(UTC)
 * @property [deletedAt] 논리 삭제 시각(UTC). null 이면 삭제되지 않은 상태
 */
@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
abstract class Base {
    abstract val id: Long?

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
        protected set


    var deletedAt: LocalDateTime? = null
        protected set

    fun isDeleted(): Boolean {
        return deletedAt != null
    }

    fun delete() {
        if (deletedAt != null) {
            throw BusinessException(CommonError.RESOURCE_ALREADY_DELETED)
        }
        deletedAt = LocalDateTime.now(ZoneId.of("UTC"))
    }
}