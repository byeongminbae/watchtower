import { type Browser, errors, type Page, type Response } from 'playwright';
import { Policy } from '../../config/policy.js';
import type { Extraction } from '../../crawl/dto/crawl-request-message.dto.js';
import { FetchedPage, PageFetchFailure, type PageFetchResult } from '../../crawl/interfaces/page-fetcher.js';
import { ImageHashRecorder } from './image-hash-recorder.js';
import type { RequestFilter } from './request-filter.js';

type ScreenshotArea = { x: number; y: number; width: number; height: number };
type ExtractedArea = { text: string; imageUrls: string[]; screenshotArea: ScreenshotArea | null };

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

  private async loadLazyImages(): Promise<void> {
    await this.page.locator('img[loading="lazy"]').evaluateAll((lazyImages: HTMLImageElement[]) => {
      for (const image of lazyImages) {
        image.loading = 'eager';
      }
    });
    await this.waitForEveryImageComplete();
  }

  private async waitForEveryImageComplete(): Promise<void> {
    try {
      await this.page.waitForFunction(() => [...document.images].every((image) => image.complete), undefined, {
        timeout: Policy.CLIENT_RENDERING_WAIT_MILLIS
      });
    } catch (error) {
      if (!(error instanceof errors.TimeoutError)) {
        throw error;
      }
    }
  }

  private async extractFetchedPage(mainResponse: Response): Promise<PageFetchResult> {
    await this.loadLazyImages();
    const area = await this.extractArea();
    if (area === null) {
      return new PageFetchFailure('INVALID_SELECTOR');
    }
    if (Buffer.byteLength(area.text) > Policy.MAX_TEXT_BYTES) {
      return new PageFetchFailure('TEXT_TOO_LARGE');
    }

    const imageHashes = (await this.imageHashRecorder?.findHashes(area.imageUrls)) ?? null;
    const screenshot = await this.takeScreenshot(area.screenshotArea);
    return new FetchedPage(mainResponse.url(), mainResponse.status(), area.text, imageHashes, screenshot);
  }

  private async takeScreenshot(area: ScreenshotArea | null): Promise<Buffer | null> {
    if (area === null) {
      return null;
    }
    const areaWithinMaxSize = {
      ...area,
      width: Math.min(area.width, Policy.MAX_SCREENSHOT_WIDTH_PIXELS),
      height: Math.min(area.height, Policy.MAX_SCREENSHOT_HEIGHT_PIXELS)
    };
    return this.page.screenshot({ fullPage: true, clip: areaWithinMaxSize, animations: 'disabled', caret: 'hide' });
  }

  /** @return 선택자 문법이 틀렸으면 null */
  private async extractArea(): Promise<ExtractedArea | null> {
    // page.evaluate 는 함수를 브라우저로 직렬화해 보내므로 바깥의 함수를 부를 수 없어서 안에서 정의함
    return this.page.evaluate((selector) => {
      const outermostAreas = findOutermostAreas(selector ?? 'body');
      if (outermostAreas === null) {
        return null;
      }
      return {
        text: joinVisibleTexts(outermostAreas),
        imageUrls: collectImageUrls(outermostAreas),
        screenshotArea:
          selector === null ? measureWholePageScreenshotArea() : measureScreenshotAreaOfVisibleAreas(outermostAreas)
      };

      /** 다른 매칭 영역 안에 들어 있지 않은 영역만 고름. 텍스트와 이미지가 중복되지 않게 함 */
      function findOutermostAreas(areaSelector: string): Element[] | null {
        try {
          const matchedAreas = [...document.querySelectorAll(areaSelector)];
          return matchedAreas.filter((area) => !area.parentElement?.closest(areaSelector));
        } catch {
          return null;
        }
      }

      function joinVisibleTexts(areas: Element[]): string {
        return areas.map((area) => (area instanceof HTMLElement ? area.innerText : '')).join('\n');
      }

      function collectImageUrls(areas: Element[]): string[] {
        const images = areas.flatMap((area) => [...area.querySelectorAll('img')]);
        return images.map((image) => image.currentSrc).filter((imageUrl) => imageUrl !== '');
      }

      function measureWholePageScreenshotArea(): ScreenshotArea {
        const page = document.documentElement;
        return { x: 0, y: 0, width: page.scrollWidth, height: page.scrollHeight };
      }

      function measureScreenshotAreaOfVisibleAreas(areas: Element[]): ScreenshotArea | null {
        const visibleRectsInViewport = areas
          .map((area) => area.getBoundingClientRect())
          .filter((rect) => rect.width > 0 && rect.height > 0);
        if (visibleRectsInViewport.length === 0) {
          return null;
        }
        const leftInPage = Math.floor(Math.min(...visibleRectsInViewport.map((rect) => rect.left)) + window.scrollX);
        const topInPage = Math.floor(Math.min(...visibleRectsInViewport.map((rect) => rect.top)) + window.scrollY);
        const rightInPage = Math.ceil(Math.max(...visibleRectsInViewport.map((rect) => rect.right)) + window.scrollX);
        const bottomInPage = Math.ceil(Math.max(...visibleRectsInViewport.map((rect) => rect.bottom)) + window.scrollY);
        return { x: leftInPage, y: topInPage, width: rightInPage - leftInPage, height: bottomInPage - topInPage };
      }
    }, this.detectionAreaSelector);
  }
}
