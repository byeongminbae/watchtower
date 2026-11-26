package kr.byeongmin.watchtower.external.oci.storage.client

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.vo.Screenshot
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * 크롤러가 스냅샷을 저장하는 OCI Object Storage 버킷. 버킷은 실행 프로파일(local, dev, prod)마다 다름
 *
 * Spring Cloud OCI 의 스토리지 기능 설정([SnapshotStorageClient.ENABLED_PROPERTY])을 끈 환경(OCI 자격 증명이 없는 CI 등)에서는 만들지 않음
 */
@Component
@ConditionalOnProperty(name = [SnapshotStorageClient.ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class OciSnapshotStorageClient(
    private val objectStorageClient: ObjectStorageClient,
    @Value("\${watchtower.snapshot-storage.namespace}") private val namespaceName: String,
    @Value("\${watchtower.snapshot-storage.bucket-name}") private val bucketName: String
) : SnapshotStorageClient {
    override fun getText(objectName: String): String {
        return getObjectBytes(objectName).toString(Charsets.UTF_8)
    }

    override fun getScreenshot(objectName: String): Screenshot {
        return Screenshot.from(getObjectBytes(objectName))
    }

    override fun saveScreenshot(objectName: String, screenshot: Screenshot) {
        val png = screenshot.toPng()
        val putObjectRequest = PutObjectRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .contentType(MediaType.IMAGE_PNG_VALUE)
            .contentLength(png.size.toLong())
            .putObjectBody(png.inputStream())
            .build()

        try {
            objectStorageClient.putObject(putObjectRequest)
        } catch (exception: BmcException) {
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    /** 앞선 정리가 객체만 지우고 실패했을 때 다시 지울 수 있도록 이미 없는 객체는 지운 것으로 봄 */
    override fun deleteObject(objectName: String) {
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .build()

        try {
            objectStorageClient.deleteObject(deleteObjectRequest)
        } catch (exception: BmcException) {
            if (isObjectNotFound(exception)) {
                return
            }
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    /**
     * [expiresAt](UTC) 까지 객체 하나만 인증 없이 읽을 수 있는 PAR(Pre-Authenticated Request) URL 을 발급함
     *
     * URL 을 가진 누구나 만료 전까지 읽을 수 있으므로 소유자를 확인한 뒤에만 발급해야 함
     */
    override fun createReadUrl(objectName: String, expiresAt: LocalDateTime): String {
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
            return objectStorageClient
                .createPreauthenticatedRequest(createPreauthenticatedRequestRequest)
                .preauthenticatedRequest
                .fullPath
        } catch (exception: BmcException) {
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    private fun getObjectBytes(objectName: String): ByteArray {
        val getObjectRequest = GetObjectRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .build()

        try {
            return objectStorageClient.getObject(getObjectRequest).inputStream.use { it.readAllBytes() }
        } catch (exception: BmcException) {
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    private fun isObjectNotFound(exception: BmcException): Boolean {
        return exception.statusCode == HttpStatus.NOT_FOUND.value()
    }
}
