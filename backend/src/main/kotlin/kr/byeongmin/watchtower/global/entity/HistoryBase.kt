package kr.byeongmin.watchtower.global.entity

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 생성 시각만 감사하는 히스토리 기록용 슈퍼클래스
 * 수정 및 삭제 방지를 강제하지는 않음
 *
 * @property [createdAt] UTC 생성 시각. @CreatedDate 가 값을 채울수 있어야 하므로 var임
 * */
@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
abstract class HistoryBase {
    abstract val id: Long?

    @CreatedDate
    var createdAt: LocalDateTime? = null
        protected set
}