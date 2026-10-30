import { Injectable } from '@nestjs/common';
import { TimeProvider } from '../../common/time-provider.js';
import type { CrawlRequestMessage } from '../dto/crawl-request-message.dto.js';
import {
  type CrawlResult,
  CrawlFailedResultMessage,
  CrawlSuccessResultMessage
} from '../dto/crawl-result-message.dto.js';
import { HtmlStorage } from '../interfaces/html-storage.interface.js';
import { PageFetcher } from '../interfaces/page-fetcher.interface.js';

@Injectable()
export class CrawlService {
  constructor(
    private readonly pageFetcher: PageFetcher,
    private readonly htmlStorage: HtmlStorage,
    private readonly timeProvider: TimeProvider
  ) {}

  async crawl(request: CrawlRequestMessage, bucketName: string): Promise<CrawlResult> {
    const startedAt = this.timeProvider.now();
    const outcome = await this.pageFetcher.fetchPage(request.url);

    if (outcome.kind === 'failed') {
      return CrawlFailedResultMessage.of(request, startedAt, this.timeProvider.now(), outcome.error);
    }

    const htmlObjectName = request.toHtmlObjectName();
    await this.htmlStorage.saveHtml(bucketName, htmlObjectName, outcome.page.html);
    return CrawlSuccessResultMessage.of(request, startedAt, this.timeProvider.now(), outcome.page, htmlObjectName);
  }
}
