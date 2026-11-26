import { Injectable } from '@nestjs/common';
import { TimeProvider } from '../common/time-provider.js';
import type { CrawlRequestMessage } from './dto/crawl-request-message.dto.js';
import { CrawlResultMessage, type PageContentObjectNames } from './dto/crawl-result-message.dto.js';
import { type FetchedPage, PageFetcher } from './interfaces/page-fetcher.js';
import type { PageContentStorage } from './interfaces/page-content-storage.js';
import { ContentHashes } from './vo/content-hashes.js';

@Injectable()
export class CrawlService {
  constructor(
    private readonly pageFetcher: PageFetcher,
    private readonly timeProvider: TimeProvider
  ) {}

  /**
   * 재시도해도 결과가 같은 실패는 실패 결과로 돌려줌
   * 브라우저 종료나 저장 실패 같은 일시적인 오류는 예외로 던져서, 요청이 완료 처리되지 않고 다시 전달되게 함
   */
  async crawl(request: CrawlRequestMessage, pageContentStorage: PageContentStorage): Promise<CrawlResultMessage> {
    const crawlingStartedAt = this.timeProvider.now();
    const fetchResult = await this.pageFetcher.fetchPage(request.url, request.extraction);
    if (fetchResult.isFailed) {
      return CrawlResultMessage.failed(request, crawlingStartedAt, this.timeProvider.now(), fetchResult.code);
    }

    const contentHashes = ContentHashes.from(fetchResult);
    const pageContentObjectNames = await this.savePageContentIfChanged(
      request,
      crawlingStartedAt,
      fetchResult,
      contentHashes,
      pageContentStorage
    );

    return CrawlResultMessage.fetched(
      request,
      crawlingStartedAt,
      this.timeProvider.now(),
      fetchResult,
      contentHashes,
      pageContentObjectNames
    );
  }

  private async savePageContentIfChanged(
    request: CrawlRequestMessage,
    crawlingStartedAt: Date,
    page: FetchedPage,
    contentHashes: ContentHashes,
    pageContentStorage: PageContentStorage
  ): Promise<PageContentObjectNames | null> {
    if (request.hasSameContentAsPrevious(contentHashes)) {
      return null;
    }
    const textObjectName = request.toTextObjectName(crawlingStartedAt);
    await pageContentStorage.saveText(textObjectName, page.text);
    const screenshotObjectName = await this.saveScreenshotIfTaken(request, crawlingStartedAt, page, pageContentStorage);
    return { textObjectName, screenshotObjectName };
  }

  private async saveScreenshotIfTaken(
    request: CrawlRequestMessage,
    crawlingStartedAt: Date,
    page: FetchedPage,
    pageContentStorage: PageContentStorage
  ): Promise<string | null> {
    if (page.screenshot === null) {
      return null;
    }
    const screenshotObjectName = request.toScreenshotObjectName(crawlingStartedAt);
    await pageContentStorage.saveScreenshot(screenshotObjectName, page.screenshot);
    return screenshotObjectName;
  }
}
