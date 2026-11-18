package kr.byeongmin.watchtower.external.oci.storage.client

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
