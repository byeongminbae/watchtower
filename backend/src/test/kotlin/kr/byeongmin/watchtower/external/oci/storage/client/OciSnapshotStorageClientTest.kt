package kr.byeongmin.watchtower.external.oci.storage.client

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.assertEquals

class OciSnapshotStorageClientTest {
    private val objectStorageClient = mock<ObjectStorageClient>()
    private val ociSnapshotStorageClient = OciSnapshotStorageClient(
        objectStorageClient = objectStorageClient,
        namespaceName = "namespace",
        bucketName = "bucket"
    )
    private val html = "<html><body>재입고 알림</body></html>"

    private fun gzip(bytes: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { it.write(bytes) }
        return outputStream.toByteArray()
    }

    @Test
    fun `크롤러가 gzip 으로 압축해 저장한 HTML 은 압축을 풀어 읽음`() {
        /** given */
        whenever(objectStorageClient.getObject(any())).thenReturn(
            GetObjectResponse.builder().inputStream(gzip(html.toByteArray()).inputStream()).build()
        )

        /** when */
        val result = ociSnapshotStorageClient.getHtml("towers/7/2026-11-04T01:00:00.000Z.html")

        /** then */
        assertEquals(html, result)
    }

    @Test
    fun `압축하지 않고 저장한 HTML 은 그대로 읽음`() {
        /** given */
        whenever(objectStorageClient.getObject(any())).thenReturn(
            GetObjectResponse.builder().inputStream(html.toByteArray().inputStream()).build()
        )

        /** when */
        val result = ociSnapshotStorageClient.getHtml("towers/7/2026-11-02T22:49:54.734Z.html")

        /** then */
        assertEquals(html, result)
    }
}
