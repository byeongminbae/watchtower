package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.tower.entity.HotTower
import kr.byeongmin.watchtower.domain.tower.entity.QHotTower.hotTower
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class HotTowerRepository(
    private val queryFactory: JPAQueryFactory,
    private val jdbcClient: JdbcClient
) {
    fun findAll(): List<HotTower> {
        return queryFactory
            .selectFrom(hotTower)
            .orderBy(hotTower.detectionCount.desc(), hotTower.domain.asc())
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
