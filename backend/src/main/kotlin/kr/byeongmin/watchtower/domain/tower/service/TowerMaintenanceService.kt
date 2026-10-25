package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.policy.MemberPolicy
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.global.config.TimeProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 더 이상 사용되지 않는 타워 관련 리소스를 정리 */
@Service
class TowerMaintenanceService(
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val towerAccessor: TowerAccessor,
    private val timeProvider: TimeProvider
) {
    // TODO: 스케줄러가 00:00 에 호출
    @Transactional
    fun deleteExpiredSnapshots(): Long {
        val now = timeProvider.now()

        // TODO: S3 에 저장된 html 및 스크린샷 객체 삭제 필요
        return towerSnapshotAccessor.deleteByCreatedAtBefore(
            createdAt = now.minus(TowerPolicy.SNAPSHOT_RETENTION_PERIOD),
            deletedAt = now
        )
    }

    // TODO: 스케줄러가 00:00 에 호출
    @Transactional
    fun pauseTowersOfInactiveMembers(): Long {
        val now = timeProvider.now()
        return towerAccessor.pauseByMemberLastSignInAtBefore(
            lastSignInBefore = now.minus(MemberPolicy.INACTIVITY_PERIOD),
            updatedAt = now
        )
    }
}
