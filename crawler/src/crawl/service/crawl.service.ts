import { Inject, Injectable } from '@nestjs/common';
import { TimeProvider } from '../../common/time-provider.js';
import type { CrawlRequestMessage } from '../dto/crawl-request-message.dto.js';
import {
  type CrawlResult,
  CrawlFailedResultMessage,
  CrawlSuccessResultMessage
} from '../dto/crawl-result-message.dto.js';
import { HTML_STORAGE, type HtmlStorage } from '../interfaces/html-storage.interface.js';
import { PAGE_FETCHER, type PageFetcher } from '../interfaces/page-fetcher.interface.js';

/**
 * 크롤링 요청 단건을 처리함. 페이지를 받아오고, 받아왔으면 HTML 을 저장한 뒤 결과 메시지를 만듦
 *
 * 큐 수신, 동시 처리 한도, 결과 발행과 요청 완료 처리는 CrawlRequestConsumer 가 맡음
 */
@Injectable()
export class CrawlService {
  constructor(
    @Inject(PAGE_FETCHER) private readonly pageFetcher: PageFetcher,
    @Inject(HTML_STORAGE) private readonly htmlStorage: HtmlStorage,
    private readonly timeProvider: TimeProvider
  ) {}

  /**
   * 재시도해도 결과가 같은 실패는 실패 결과로 돌려줌
   * 브라우저 종료나 저장 실패 같은 일시적인 오류는 예외로 던져서, 요청이 완료 처리되지 않고 다시 전달되게 함
   */
  async crawl(crawlRequest: CrawlRequestMessage, bucketName: string): Promise<CrawlResult> {
    const crawlStartedAt = this.timeProvider.now();
    const pageFetchResult = await this.pageFetcher.fetchPage(crawlRequest.url);

    if (pageFetchResult.isFailed()) {
      return new CrawlFailedResultMessage(crawlRequest, crawlStartedAt, this.timeProvider.now(), pageFetchResult.error);
    }

    const htmlObjectName = crawlRequest.toHtmlObjectName();
    await this.htmlStorage.saveHtml(bucketName, htmlObjectName, pageFetchResult.page.html);

    return new CrawlSuccessResultMessage(
      crawlRequest,
      crawlStartedAt,
      this.timeProvider.now(),
      pageFetchResult.page,
      htmlObjectName
    );
  }
}
