package kr.byeongmin.watchtower.external.oci.storage.client

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 크롤러가 스냅샷을 저장하는 OCI Object Storage 버킷. 버킷은 실행 프로파일(local, dev, prod)마다 다름
 *
 * Spring Cloud OCI 의 스토리지 기능 설정([STORAGE_ENABLED_PROPERTY])을 끈 환경(OCI 자격 증명이 없는 CI 등)에서는 만들지 않음
 */
@Component
@ConditionalOnProperty(name = [OciSnapshotStorageClient.STORAGE_ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class OciSnapshotStorageClient(
    private val objectStorageClient: ObjectStorageClient,
    @Value("\${watchtower.snapshot-storage.namespace}") private val namespaceName: String,
    @Value("\${watchtower.snapshot-storage.bucket-name}") private val bucketName: String
) {
    /** 크롤러가 UTF-8 로 저장한 HTML 을 읽음 */
    fun getHtml(objectName: String): String {
        val getObjectRequest = GetObjectRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .build()

        try {
            return objectStorageClient.getObject(getObjectRequest).inputStream.use {
                it.readAllBytes().toString(Charsets.UTF_8)
            }
        } catch (exception: BmcException) {
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    companion object {
        /** Spring Cloud OCI 가 스토리지 자동 설정을 켜고 끄는 설정. 기본값은 true */
        const val STORAGE_ENABLED_PROPERTY = "spring.cloud.oci.storage.enabled"
    }
}
