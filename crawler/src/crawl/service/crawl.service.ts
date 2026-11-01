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

@Injectable()
export class CrawlService {
  constructor(
    @Inject(PAGE_FETCHER) private readonly pageFetcher: PageFetcher,
    @Inject(HTML_STORAGE) private readonly htmlStorage: HtmlStorage,
    private readonly timeProvider: TimeProvider
  ) {}

  async crawl(crawlRequest: CrawlRequestMessage, bucketName: string): Promise<CrawlResult> {
    const crawlStartedAt = this.timeProvider.now();
    const fetchOutcome = await this.pageFetcher.fetchPage(crawlRequest.url);

    if (fetchOutcome.kind === 'failed') {
      return CrawlFailedResultMessage.of(crawlRequest, crawlStartedAt, this.timeProvider.now(), fetchOutcome.error);
    }

    const htmlObjectName = crawlRequest.toHtmlObjectName();
    await this.htmlStorage.saveHtml(bucketName, htmlObjectName, fetchOutcome.page.html);
    return CrawlSuccessResultMessage.of(
      crawlRequest,
      crawlStartedAt,
      this.timeProvider.now(),
      fetchOutcome.page,
      htmlObjectName
    );
  }
}
