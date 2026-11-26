package kr.byeongmin.watchtower.external.oci.storage.client

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import kr.byeongmin.watchtower.domain.tower.vo.Screenshot
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class OciSnapshotStorageClientTest {
    private val objectStorageClient = mock<ObjectStorageClient>()
    private val ociSnapshotStorageClient = OciSnapshotStorageClient(
        objectStorageClient = objectStorageClient,
        namespaceName = "namespace",
        bucketName = "bucket"
    )

    @Test
    fun `크롤러가 UTF-8 평문으로 저장한 텍스트를 그대로 읽음`() {
        /** given */
        val text = "재입고 알림"
        whenever(objectStorageClient.getObject(any())).thenReturn(
            GetObjectResponse.builder().inputStream(text.toByteArray().inputStream()).build()
        )

        /** when */
        val result = ociSnapshotStorageClient.getText("towers/7/2026-11-04T01:00:00.000Z.txt")

        /** then */
        assertEquals(text, result)
    }

    @Test
    fun `스크린샷은 PNG 형식으로 저장함`() {
        /** given */
        val png = byteArrayOf(1, 2, 3)
        val screenshot = mock<Screenshot> {
            on { toPng() } doReturn png
        }

        /** when */
        ociSnapshotStorageClient.saveScreenshot("towers/7/2026-11-04T01:00:00.000Z-highlighted.png", screenshot)

        /** then */
        val putObjectRequest = argumentCaptor<PutObjectRequest>()
        verify(objectStorageClient).putObject(putObjectRequest.capture())
        assertEquals("towers/7/2026-11-04T01:00:00.000Z-highlighted.png", putObjectRequest.firstValue.objectName)
        assertEquals("image/png", putObjectRequest.firstValue.contentType)
        assertContentEquals(png, putObjectRequest.firstValue.putObjectBody.readAllBytes())
    }

    @Test
    fun `읽기 URL 은 OCI 가 발급한 PAR 의 전체 경로를 그대로 돌려줌`() {
        /** given */
        val fullPath = "https://namespace.objectstorage.ap-osaka-1.oci.customer-oci.com/p/token/n/namespace/b/bucket/o/towers/7/1.png"
        whenever(objectStorageClient.createPreauthenticatedRequest(any())).thenReturn(
            CreatePreauthenticatedRequestResponse.builder()
                .preauthenticatedRequest(
                    PreauthenticatedRequest.builder()
                        .accessUri("/p/token/n/namespace/b/bucket/o/towers/7/1.png")
                        .fullPath(fullPath)
                        .build()
                )
                .build()
        )

        /** when */
        val readUrl = ociSnapshotStorageClient.createReadUrl("towers/7/1.png", LocalDateTime.of(2026, 11, 4, 1, 10))

        /** then */
        assertEquals(fullPath, readUrl)
    }

    @Test
    fun `이미 없는 객체를 지우면 지운 것으로 보고 넘어감`() {
        /** given */
        whenever(objectStorageClient.deleteObject(any()))
            .thenThrow(BmcException(404, "ObjectNotFound", "없음", "request-id"))

        /** when, then */
        assertDoesNotThrow { ociSnapshotStorageClient.deleteObject("towers/7/2026-11-04T01:00:00.000Z.txt") }
    }

    @Test
    fun `객체를 지우다 404 외의 오류가 나면 외부 API 오류로 실패함`() {
        /** given */
        whenever(objectStorageClient.deleteObject(any()))
            .thenThrow(BmcException(500, "InternalServerError", "오류", "request-id"))

        /** when */
        val exception = assertThrows<BusinessException> {
            ociSnapshotStorageClient.deleteObject("towers/7/2026-11-04T01:00:00.000Z.txt")
        }

        /** then */
        assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
    }
}
