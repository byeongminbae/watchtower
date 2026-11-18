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
import kr.byeongmin.watchtower.domain.tower.client.CrawlQueueClient
import kr.byeongmin.watchtower.domain.tower.dto.CrawlRequestMessageDto
import kr.byeongmin.watchtower.domain.tower.dto.QueueResultMessageDto
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 크롤러와 주고받는 OCI Queue. 큐는 실행 프로파일(local, dev, prod)마다 다름
 *
 * Spring Cloud OCI 의 큐 기능 설정([CrawlQueueClient.ENABLED_PROPERTY])을 끈 환경(OCI 자격 증명이 없는 CI 등)에서는 만들지 않음
 */
@Component
@ConditionalOnProperty(name = [CrawlQueueClient.ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class OciQueueClient(
    private val queueClient: QueueClient,
    private val objectMapper: ObjectMapper,
    @Value("\${watchtower.crawl-queue.request-queue-id}") private val requestQueueId: String,
    @Value("\${watchtower.crawl-queue.result-queue-id}") private val resultQueueId: String
) : CrawlQueueClient {
    /** 요청을 한 번에 넣음. OCI Queue 는 일부만 넣지 않고, 실패하면 모두 넣지 않음 */
    override fun sendCrawlRequests(crawlRequestMessageDtos: List<CrawlRequestMessageDto>) {
        if (crawlRequestMessageDtos.size > CrawlQueueClient.MAX_BATCH_SIZE) {
            throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)
        }

        val putMessagesRequest = PutMessagesRequest.builder()
            .queueId(requestQueueId)
            .putMessagesDetails(
                PutMessagesDetails.builder()
                    .messages(
                        crawlRequestMessageDtos.map {
                            PutMessagesDetailsEntry.builder()
                                .content(objectMapper.writeValueAsString(it))
                                .build()
                        }
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
    override fun fetchCrawlResults(): List<QueueResultMessageDto> {
        val getMessagesRequest = GetMessagesRequest.builder()
            .queueId(resultQueueId)
            .timeoutInSeconds(LONG_POLLING_SECONDS)
            .limit(CrawlQueueClient.MAX_BATCH_SIZE)
            .build()

        return queueClient.getMessages(getMessagesRequest).getMessages.messages.map {
            QueueResultMessageDto(receipt = it.receipt, content = it.content)
        }
    }

    /** 지우지 못한 메시지는 visibility 시간이 지나면 다시 전달됨 */
    override fun deleteCrawlResults(receipts: List<String>) {
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
        /**
         * 큐가 비어 있을 때 받기 요청 하나가 메시지를 기다리는 최대 시간(OCI Queue 최대값). 메시지가 하나라도 있으면 바로 응답함
         *
         * 짧으면 빈 큐에 대한 요청 수(비용)가 늘고, 길면 애플리케이션 종료가 그만큼 늦어짐
         */
        const val LONG_POLLING_SECONDS = 30
    }
}
