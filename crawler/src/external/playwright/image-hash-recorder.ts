import type { Page, Response } from 'playwright';
import { sha256 } from '../../common/sha256.js';

/** 이미지를 다시 요청하지 않고 브라우저가 받은 응답 본문으로 해시를 계산함 */
export class ImageHashRecorder {
  private readonly hashesByUrl = new Map<string, string>();
  private readonly pendingRecords: Promise<void>[] = [];

  static start(page: Page): ImageHashRecorder {
    const imageHashRecorder = new ImageHashRecorder();
    page.on('response', (response) => {
      if (ImageHashRecorder.isReceivedImage(response)) {
        imageHashRecorder.pendingRecords.push(imageHashRecorder.record(response));
      }
    });
    return imageHashRecorder;
  }

  /** 받지 못했거나 막은 주소(data: URL, 내부 주소 등)는 결과에서 빠짐 */
  async findHashes(imageUrls: readonly string[]): Promise<string[]> {
    await Promise.all(this.pendingRecords);
    return imageUrls.flatMap((imageUrl) => this.hashesByUrl.get(imageUrl) ?? []);
  }

  private static isReceivedImage(response: Response): boolean {
    return response.request().resourceType() === 'image' && response.ok();
  }

  private async record(response: Response): Promise<void> {
    try {
      this.hashesByUrl.set(response.url(), sha256(await response.body()));
    } catch {
      // 리다이렉트 응답이나 페이지를 닫는 중에 받은 응답은 본문이 없어서 건너뜀
    }
  }
}
