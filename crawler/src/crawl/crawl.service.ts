import { Inject, Injectable } from '@nestjs/common';
import { TimeProvider } from '../common/time-provider.js';
import type { CrawlRequest } from './crawl-request.js';
import { type CrawlResult, FailedCrawlResult, FetchedCrawlResult } from './crawl-result.js';
import { HTML_STORAGE, type HtmlStorage } from './interfaces/html-storage.interface.js';
import { PAGE_FETCHER, type PageFetcher } from './interfaces/page-fetcher.interface.js';

@Injectable()
export class CrawlService {
  constructor(
    @Inject(PAGE_FETCHER) private readonly pageFetcher: PageFetcher,
    @Inject(HTML_STORAGE) private readonly htmlStorage: HtmlStorage,
    private readonly timeProvider: TimeProvider
  ) {}

  async crawl(request: CrawlRequest, bucketName: string): Promise<CrawlResult> {
    const startedAt = this.timeProvider.now();
    const outcome = await this.pageFetcher.fetchPage(request.url);

    if (outcome.kind === 'failed') {
      return FailedCrawlResult.of(request, startedAt, this.timeProvider.now(), outcome.error);
    }

    const htmlObjectName = request.toHtmlObjectName();
    await this.htmlStorage.saveHtml(bucketName, htmlObjectName, outcome.page.html);
    return FetchedCrawlResult.of(request, startedAt, this.timeProvider.now(), outcome.page, htmlObjectName);
  }
}
