import { FetchedPage } from '../../../src/crawl/interfaces/page-fetcher.js';
import { ContentHashes } from '../../../src/crawl/vo/content-hashes.js';

function pageWithImageHashes(imageHashes: string[] | null): FetchedPage {
  return new FetchedPage('https://example.com', 200, '재입고 알림', imageHashes, null);
}

describe('ContentHashes', () => {
  it('이미지 해시 목록은 순서와 상관없이 정렬해서 하나의 해시로 합침', () => {
    // given
    const pages = [
      pageWithImageHashes(['a'.repeat(64), 'b'.repeat(64)]),
      pageWithImageHashes(['b'.repeat(64), 'a'.repeat(64)])
    ];

    // when
    const imageHashes = pages.map((page) => ContentHashes.from(page).imageHash);

    // then
    expect(imageHashes).toEqual([
      '5e9ae866add9a85d69c3481d059bb9f158a39e5670ba11f95112fc409630894e',
      '5e9ae866add9a85d69c3481d059bb9f158a39e5670ba11f95112fc409630894e'
    ]);
  });

  it('이미지를 수집하지 않았으면 합친 해시가 없고 이미지가 없으면 빈 목록의 해시가 됨', () => {
    // given
    const notCollected = pageWithImageHashes(null);
    const noImage = pageWithImageHashes([]);

    // when
    const notCollectedImageHash = ContentHashes.from(notCollected).imageHash;
    const noImageImageHash = ContentHashes.from(noImage).imageHash;

    // then
    expect(notCollectedImageHash).toBeNull();
    expect(noImageImageHash).toBe('e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855');
  });
});
