package kr.byeongmin.watchtower.external.oci.queue.client

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.queue.QueueClient
import com.oracle.bmc.queue.model.DeleteMessagesDetails
import com.oracle.bmc.queue.model.DeleteMessagesDetailsEntry
import com.oracle.bmc.queue.model.PutMessagesDetails
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry
import com.oracle.bmc.queue.requests.DeleteMessagesRequest
import com.oracle.bmc.queue.requests.GetMessagesRequest
import com.oracle.bmc.queue.requests.PutMessagesRequest
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlRequestDto
import kr.byeongmin.watchtower.external.oci.queue.dto.QueueResultMessageDto
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 크롤러와 주고받는 OCI Queue. 큐는 실행 프로파일(local, dev, prod)마다 다름
 *
 * Spring Cloud OCI 의 큐 기능 설정([QUEUE_ENABLED_PROPERTY])을 끈 환경(OCI 자격 증명이 없는 CI 등)에서는 만들지 않음
 */
@Component
@ConditionalOnProperty(name = [OciQueueClient.QUEUE_ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class OciQueueClient(
    private val queueClient: QueueClient,
    private val objectMapper: ObjectMapper,
    @Value("\${watchtower.crawl-queue.request-queue-id}") private val requestQueueId: String,
    @Value("\${watchtower.crawl-queue.result-queue-id}") private val resultQueueId: String
) {
    fun sendCrawlRequest(crawlRequestDto: CrawlRequestDto) {
        val putMessagesRequest = PutMessagesRequest.builder()
            .queueId(requestQueueId)
            .putMessagesDetails(
                PutMessagesDetails.builder()
                    .messages(
                        listOf(
                            PutMessagesDetailsEntry.builder()
                                .content(objectMapper.writeValueAsString(crawlRequestDto))
                                .build()
                        )
                    )
                    .build()
            )
            .build()

        try {
            queueClient.putMessages(putMessagesRequest)
        } catch (exception: BmcException) {
            throw BusinessException(error = CommonError.EXTERNAL_API_ERROR, cause = exception)
        }
    }

    /** 결과가 하나라도 있으면 바로, 없으면 [LONG_POLLING_SECONDS] 동안 기다렸다가 빈 목록을 돌려줌 */
    fun fetchCrawlResults(): List<QueueResultMessageDto> {
        val getMessagesRequest = GetMessagesRequest.builder()
            .queueId(resultQueueId)
            .timeoutInSeconds(LONG_POLLING_SECONDS)
            .limit(MAX_BATCH_SIZE)
            .build()

        return queueClient.getMessages(getMessagesRequest).getMessages.messages.map {
            QueueResultMessageDto(receipt = it.receipt, content = it.content)
        }
    }

    /** 지우지 못한 메시지는 visibility 시간이 지나면 다시 전달됨 */
    fun deleteCrawlResults(receipts: List<String>) {
        val deleteMessagesRequest = DeleteMessagesRequest.builder()
            .queueId(resultQueueId)
            .deleteMessagesDetails(
                DeleteMessagesDetails.builder()
                    .entries(receipts.map { DeleteMessagesDetailsEntry.builder().receipt(it).build() })
                    .build()
            )
            .build()

        queueClient.deleteMessages(deleteMessagesRequest)
    }

    companion object {
        /** Spring Cloud OCI 가 큐 자동 설정을 켜고 끄는 설정. 기본값은 true */
        const val QUEUE_ENABLED_PROPERTY = "spring.cloud.oci.queue.enabled"

        /**
         * 큐가 비어 있을 때 받기 요청 하나가 메시지를 기다리는 최대 시간(OCI Queue 최대값). 메시지가 하나라도 있으면 바로 응답함
         *
         * 짧으면 빈 큐에 대한 요청 수(비용)가 늘고, 길면 애플리케이션 종료가 그만큼 늦어짐
         */
        const val LONG_POLLING_SECONDS = 30

        /**
         * OCI Queue 가 받기, 지우기 요청 한 번에 처리할 수 있는 최대 메시지 수. 요청 수로 과금되므로 최대로 묶음
         *
         * 받기는 이 수가 찰 때까지 기다리지 않고, 응답하는 순간 큐에 있는 메시지를 이 수까지 담아 줌
         */
        const val MAX_BATCH_SIZE = 20
    }
}
