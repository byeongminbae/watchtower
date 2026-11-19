import { Injectable, type OnModuleInit } from '@nestjs/common';
import { type Browser, chromium, type LaunchOptions } from 'playwright';
import type { Extraction } from '../../crawl/dto/crawl-request-message.dto.js';
import { PageFetcher, PageFetchFailure, type PageFetchResult } from '../../crawl/interfaces/page-fetcher.js';
import { CrawlHostValidator } from '../../network/crawl-host-validator.js';
import { DnsResolver } from '../../network/dns-resolver.js';
import { BrowserTab } from './browser-tab.js';
import { RequestFilter } from './request-filter.js';

/** 크롤링마다 브라우저를 새로 띄워 쿠키, 저장소, 브라우저 프로세스가 다른 크롤링과 섞이지 않게 함 */
@Injectable()
export class PlaywrightPageFetcher implements PageFetcher, OnModuleInit {
  private static readonly BROWSER_LAUNCH_OPTIONS: LaunchOptions = {
    /**
     * Chromium 샌드박스. Playwright 는 기본으로 끄는데, 꺼져 있으면 렌더러의 V8 취약점 하나로 크롤러와 같은 사용자 권한을 얻어
     * ~/.oci 의 API 키 파일과 크롤러 프로세스 메모리까지 읽을 수 있음
     * 샌드박스에 필요한 user namespace 생성을 Docker 기본 seccomp 와 Ubuntu AppArmor 가 막아서, 호스트에서 둘 다 풀어야 함(infra/crawler-sandbox.sh)
     */
    chromiumSandbox: true,
    /**
     * Chromium 에 넘기는 환경변수. 지정하지 않으면 크롤러의 환경변수 전체가 넘어감
     * 샌드박스 안의 렌더러는 파일을 읽지 못해도 자기 프로세스의 환경변수는 읽을 수 있어서, 실행에 필요한 값만 넘김
     */
    env: { HOME: '/tmp', PATH: '/usr/local/bin:/usr/bin:/bin' }
  };

  constructor(private readonly dnsResolver: DnsResolver) {}

  /**
   * 시작할 때 브라우저를 한 번 띄워 봄. 샌드박스를 띄울 수 없는 환경이면 기동을 멈춤
   * 그래야 배포 헬스체크가 실패해 이전 크롤러가 계속 돌고, 모든 크롤링이 실패하는 크롤러가 배포되지 않음
   */
  async onModuleInit(): Promise<void> {
    const browser = await PlaywrightPageFetcher.launchBrowser();
    await browser.close();
  }

  async fetchPage(url: string, extraction: Extraction): Promise<PageFetchResult> {
    const hostValidator = new CrawlHostValidator(this.dnsResolver);
    if (!(await hostValidator.isAllowedUrl(url))) {
      return new PageFetchFailure('URL_NOT_ALLOWED');
    }

    await using browser = await PlaywrightPageFetcher.launchBrowser();
    const requestFilter = new RequestFilter(hostValidator, extraction.collectImageHashes);
    const browserTab = await BrowserTab.open(browser, requestFilter, extraction);
    return await browserTab.fetch(url);
  }

  private static launchBrowser(): Promise<Browser> {
    return chromium.launch(PlaywrightPageFetcher.BROWSER_LAUNCH_OPTIONS);
  }
}
