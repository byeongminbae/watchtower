package kr.byeongmin.watchtower.global.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** JPA EntityManager를 사용하는 공용 QueryDSL 쿼리 팩토리를 등록하는 설정 클래스 */
@Configuration
class QuerydslConfig(
    private val entityManager: EntityManager
) {
    @Bean
    fun jpaQueryFactory(): JPAQueryFactory {
        return JPAQueryFactory(entityManager)
    }
}
