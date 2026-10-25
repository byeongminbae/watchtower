package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.tower.dto.HotTowerResponseDto
import kr.byeongmin.watchtower.domain.tower.entity.QHotTower.hotTower
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class HotTowerRepository(
    private val queryFactory: JPAQueryFactory,
    private val jdbcClient: JdbcClient
) {
    fun findAll(): List<HotTowerResponseDto> {
        return queryFactory
            .select(
                Projections.constructor(
                    HotTowerResponseDto::class.java,
                    hotTower.domain,
                    hotTower.url,
                    hotTower.changeCount
                )
            )
            .from(hotTower)
            .orderBy(hotTower.changeCount.desc(), hotTower.domain.asc())
            .fetch()
    }

    /**
     * MATERIALIZED VIEW 는 querydsl 로 풀수가 없어서 예외적으로 네이티브 SQL 로 작성함.
     */
    fun refresh() {
        jdbcClient
            .sql("REFRESH MATERIALIZED VIEW CONCURRENTLY hot_tower")
            .update()
    }
}
