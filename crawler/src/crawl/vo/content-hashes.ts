import { sha256 } from '../../common/sha256.js';
import type { FetchedPage } from '../interfaces/page-fetcher.js';
import { DateTimeRemover } from '../text/date-time-remover.js';

export class ContentHashes {
  constructor(
    readonly textHash: string,
    readonly dateTimeIgnoredTextHash: string,
    readonly imageHash: string | null
  ) {}

  static from(page: FetchedPage): ContentHashes {
    return new ContentHashes(
      sha256(page.text),
      sha256(DateTimeRemover.remove(page.text)),
      ContentHashes.toImageHash(page.imageHashes)
    );
  }

  private static toImageHash(imageHashes: readonly string[] | null): string | null {
    if (imageHashes === null) {
      return null;
    }
    return sha256(imageHashes.toSorted().join('\n'));
  }

  selectTextHash(ignoreDateTime: boolean): string {
    if (ignoreDateTime) {
      return this.dateTimeIgnoredTextHash;
    }
    return this.textHash;
  }
}
