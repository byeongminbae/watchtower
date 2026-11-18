import { Injectable } from '@nestjs/common';
import { TimeProvider } from '../common/time-provider.js';
import type { CrawlRequestMessage } from './dto/crawl-request-message.dto.js';
import { CrawlResultMessage } from './dto/crawl-result-message.dto.js';
import { type FetchedPage, PageFetcher } from './interfaces/page-fetcher.js';
import type { SnapshotStorage } from './interfaces/snapshot-storage.js';
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
  async crawl(request: CrawlRequestMessage, snapshotStorage: SnapshotStorage): Promise<CrawlResultMessage> {
    const crawlingStartedAt = this.timeProvider.now();
    const fetchResult = await this.pageFetcher.fetchPage(request.url, request.extraction);
    if (fetchResult.isFailed) {
      return CrawlResultMessage.failed(request, crawlingStartedAt, this.timeProvider.now(), fetchResult.code);
    }

    const contentHashes = ContentHashes.from(fetchResult);
    const textObjectName = await this.saveTextIfContentChanged(
      request,
      crawlingStartedAt,
      fetchResult,
      contentHashes,
      snapshotStorage
    );

    return CrawlResultMessage.fetched(
      request,
      crawlingStartedAt,
      this.timeProvider.now(),
      fetchResult,
      contentHashes,
      textObjectName
    );
  }

  private async saveTextIfContentChanged(
    request: CrawlRequestMessage,
    crawlingStartedAt: Date,
    page: FetchedPage,
    contentHashes: ContentHashes,
    snapshotStorage: SnapshotStorage
  ): Promise<string | null> {
    if (request.hasSameContentAsPrevious(contentHashes)) {
      return null;
    }
    const textObjectName = request.toTextObjectName(crawlingStartedAt);
    await snapshotStorage.saveText(textObjectName, page.text);
    return textObjectName;
  }
}
