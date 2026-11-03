package kr.byeongmin.watchtower.external.oci.storage.client

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

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

    /**
     * [expiresAt](UTC) 까지 객체 하나만 인증 없이 읽을 수 있는 PAR(Pre-Authenticated Request) URL 을 발급함
     *
     * URL 을 가진 누구나 만료 전까지 읽을 수 있으므로 소유자를 확인한 뒤에만 발급해야 함
     */
    fun createReadUrl(objectName: String, expiresAt: LocalDateTime): String {
        val createPreauthenticatedRequestRequest = CreatePreauthenticatedRequestRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .createPreauthenticatedRequestDetails(
                CreatePreauthenticatedRequestDetails.builder()
                    .name(objectName)
                    .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                    .objectName(objectName)
                    .timeExpires(Date.from(expiresAt.toInstant(ZoneOffset.UTC)))
                    .build()
            )
            .build()

        try {
            val preauthenticatedRequest = objectStorageClient
                .createPreauthenticatedRequest(createPreauthenticatedRequestRequest)
                .preauthenticatedRequest
            return objectStorageClient.endpoint + preauthenticatedRequest.accessUri
        } catch (exception: BmcException) {
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    companion object {
        /** Spring Cloud OCI 가 스토리지 자동 설정을 켜고 끄는 설정. 기본값은 true */
        const val STORAGE_ENABLED_PROPERTY = "spring.cloud.oci.storage.enabled"
    }
}
