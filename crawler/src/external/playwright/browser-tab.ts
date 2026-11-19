import { type Browser, errors, type Page, type Response } from 'playwright';
import { Policy } from '../../config/policy.js';
import type { Extraction } from '../../crawl/dto/crawl-request-message.dto.js';
import { FetchedPage, PageFetchFailure, type PageFetchResult } from '../../crawl/interfaces/page-fetcher.js';
import { ImageHashRecorder } from './image-hash-recorder.js';
import type { RequestFilter } from './request-filter.js';

type ExtractedArea = { text: string; imageUrls: string[] };

export class BrowserTab {
  private constructor(
    private readonly browser: Browser,
    private readonly page: Page,
    private readonly requestFilter: RequestFilter,
    private readonly detectionAreaSelector: string | null,
    private readonly imageHashRecorder: ImageHashRecorder | null
  ) {}

  static async open(browser: Browser, requestFilter: RequestFilter, extraction: Extraction): Promise<BrowserTab> {
    const page = await requestFilter.openFilteredPage(browser);
    const imageHashRecorder = extraction.collectImageHashes ? ImageHashRecorder.start(page) : null;
    return new BrowserTab(browser, page, requestFilter, extraction.detectionAreaSelector, imageHashRecorder);
  }

  async fetch(url: string): Promise<PageFetchResult> {
    let mainResponse: Response | null;
    try {
      mainResponse = await this.page.goto(url, { waitUntil: 'load', timeout: Policy.NAVIGATION_TIMEOUT_MILLIS });
    } catch (error) {
      // 브라우저가 죽은 경우만 일시적인 오류로 보고 예외를 던져 메시지를 다시 전달받음
      if (!this.browser.isConnected()) {
        throw error;
      }
      if (error instanceof errors.TimeoutError) {
        return new PageFetchFailure('NAVIGATION_TIMEOUT');
      }
      return new PageFetchFailure('NAVIGATION_FAILED');
    }

    // about:blank 나 같은 문서 안의 해시 이동처럼 응답이 없는 탐색이면 null 이 옴
    if (mainResponse === null) {
      return new PageFetchFailure('NAVIGATION_FAILED');
    }
    if (!(await this.requestFilter.isEveryRedirectUrlAllowed(mainResponse))) {
      return new PageFetchFailure('URL_NOT_ALLOWED');
    }

    await this.waitForClientRendering();
    return this.extractFetchedPage(mainResponse);
  }

  private async waitForClientRendering(): Promise<void> {
    try {
      await this.page.waitForLoadState('networkidle', { timeout: Policy.CLIENT_RENDERING_WAIT_MILLIS });
    } catch (error) {
      if (!(error instanceof errors.TimeoutError)) {
        throw error;
      }
    }
  }

  private async extractFetchedPage(mainResponse: Response): Promise<PageFetchResult> {
    await this.imageHashRecorder?.loadLazyImages();
    const area = await this.extractArea();
    if (area === null) {
      return new PageFetchFailure('INVALID_SELECTOR');
    }
    if (Buffer.byteLength(area.text) > Policy.MAX_TEXT_BYTES) {
      return new PageFetchFailure('TEXT_TOO_LARGE');
    }

    const imageHashes = (await this.imageHashRecorder?.findHashes(area.imageUrls)) ?? null;
    return new FetchedPage(mainResponse.url(), mainResponse.status(), area.text, imageHashes);
  }

  /** @return 선택자 문법이 틀렸으면 null */
  private async extractArea(): Promise<ExtractedArea | null> {
    // page.evaluate 는 함수를 브라우저로 직렬화해 보내므로 바깥의 함수를 부를 수 없어서 안에서 정의함
    return this.page.evaluate((selector) => {
      const matchedAreas = findAreas();
      if (matchedAreas === null) {
        return null;
      }
      const outermostAreas = matchedAreas.filter((area) => !hasMatchedAncestor(area));
      return { text: joinVisibleTexts(outermostAreas), imageUrls: collectImageUrls(outermostAreas) };

      function findAreas(): Element[] | null {
        try {
          return [...document.querySelectorAll(selector)];
        } catch {
          return null;
        }
      }

      function hasMatchedAncestor(area: Element): boolean {
        return area.parentElement !== null && area.parentElement.closest(selector) !== null;
      }

      function joinVisibleTexts(areas: Element[]): string {
        return areas.map((area) => (area instanceof HTMLElement ? area.innerText : '')).join('\n');
      }

      function collectImageUrls(areas: Element[]): string[] {
        const images = areas.flatMap((area) => [...area.querySelectorAll('img')]);
        return images.map((image) => image.currentSrc).filter((imageUrl) => imageUrl !== '');
      }
    }, this.detectionAreaSelector ?? 'body');
  }
}
