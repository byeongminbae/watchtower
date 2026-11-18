package kr.byeongmin.watchtower.domain.tower.client

import kr.byeongmin.watchtower.domain.tower.dto.CrawlRequestMessageDto
import kr.byeongmin.watchtower.domain.tower.dto.QueueResultMessageDto

interface CrawlQueueClient {
    fun sendCrawlRequests(crawlRequestMessageDtos: List<CrawlRequestMessageDto>)

    fun fetchCrawlResults(): List<QueueResultMessageDto>

    fun deleteCrawlResults(receipts: List<String>)

    companion object {
        /** Spring Cloud OCI 가 큐 자동 설정을 켜고 끄는 설정. 기본값은 true */
        const val ENABLED_PROPERTY = "spring.cloud.oci.queue.enabled"

        /**
         * OCI Queue 가 넣기, 받기, 지우기 요청 한 번에 처리할 수 있는 최대 메시지 수. 요청 수로 과금되므로 최대로 묶음
         *
         * 받기는 이 수가 찰 때까지 기다리지 않고, 응답하는 순간 큐에 있는 메시지를 이 수까지 담아 줌
         */
        const val MAX_BATCH_SIZE = 20
    }
}
