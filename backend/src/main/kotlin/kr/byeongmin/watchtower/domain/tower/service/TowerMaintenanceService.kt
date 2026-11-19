package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.policy.MemberPolicy
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/** 더 이상 사용되지 않는 타워 관련 리소스를 정리 */
@Service
@ConditionalOnProperty(name = [SnapshotStorageClient.ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class TowerMaintenanceService(
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val towerAccessor: TowerAccessor,
    private val snapshotStorageClient: SnapshotStorageClient,
    private val transactionTemplate: TransactionTemplate,
    private val timeProvider: TimeProvider
) {
    /**
     * 객체를 먼저 지워서 DB 삭제가 실패해도 다음 실행에서 다시 지울 수 있게 함
     * 반대 순서면 객체 삭제가 실패했을 때 객체를 가리키는 스냅샷이 없어 영영 지우지 못함
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun deleteExpiredSnapshots(): Long {
        val now = timeProvider.now()
        val createdBefore = now.minus(TowerPolicy.SNAPSHOT_RETENTION_PERIOD)

        var deletedCount = 0L
        while (true) { // TODO: 읽기 불편함. 개선해야할듯
            val expiredSnapshots = towerSnapshotAccessor.findExpiredSnapshotsExceptLatest(
                createdBefore = createdBefore,
                limit = TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE
            )
            if (expiredSnapshots.isEmpty()) {
                return deletedCount
            }

            expiredSnapshots
                .flatMap { it.getAllObjectNames() }
                .forEach { snapshotStorageClient.deleteObject(it) }

            deletedCount += transactionTemplate.execute {
                towerSnapshotAccessor.deleteSnapshots(
                    snapshotIds = expiredSnapshots.map { it.id.ifNullThrow() },
                    deletedAt = now
                )
            }.ifNullThrow()
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun pauseTowersOfInactiveMembers(): Long {
        val now = timeProvider.now()
        return towerAccessor.pauseTowersOfInactiveMembers(
            lastSignInBefore = now.minus(MemberPolicy.INACTIVITY_PERIOD),
            updatedAt = now
        )
    }
}
