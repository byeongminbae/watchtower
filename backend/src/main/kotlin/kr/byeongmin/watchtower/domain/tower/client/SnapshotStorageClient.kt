package kr.byeongmin.watchtower.domain.tower.client

import kr.byeongmin.watchtower.domain.tower.vo.Screenshot
import java.time.LocalDateTime

interface SnapshotStorageClient {
    fun getText(objectName: String): String

    fun getScreenshot(objectName: String): Screenshot

    fun saveScreenshot(objectName: String, screenshot: Screenshot)

    fun deleteObject(objectName: String)

    fun createReadUrl(objectName: String, expiresAt: LocalDateTime): String

    companion object {
        /** Spring Cloud OCI 가 스토리지 자동 설정을 켜고 끄는 설정. 기본값은 true */
        const val ENABLED_PROPERTY = "spring.cloud.oci.storage.enabled"
    }
}
