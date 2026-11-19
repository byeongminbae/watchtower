import { DateTimeRemover } from '../../../src/crawl/text/date-time-remover.js';

describe('DateTimeRemover', () => {
  it('연도가 붙은 날짜와 시각 표기를 지움', () => {
    for (const dateTime of [
      '2026-11-06',
      '2026.11.06',
      '2026/11/06',
      '2026. 11. 6.',
      '2026-11-06T01:00:00.000Z',
      '2026-11-06 14:30',
      '2026년 11월 6일',
      '11월 6일 (금)',
      '14:30',
      '14:30:05',
      '오후 2:30',
      '2:30 PM',
      '3분 전',
      '2시간 전',
      '5 minutes ago',
      '방금 전'
    ]) {
      // given
      const text = `공지[${dateTime}]`;

      // when
      const result = DateTimeRemover.remove(text);

      // then
      expect(result, dateTime).toBe('공지[]');
    }
  });

  it('줄바꿈 없는 공백이나 전각 공백이 섞인 날짜와 시각 표기도 지움', () => {
    for (const dateTime of [
      '2026. 11. 6.',
      '2026-11-06 14:30',
      '11월 6일　(금)',
      '오후　2:30',
      '3분 전',
      '5 minutes ago',
      '방금 전'
    ]) {
      // given
      const text = `공지[${dateTime}]`;

      // when
      const result = DateTimeRemover.remove(text);

      // then
      expect(result, dateTime).toBe('공지[]');
    }
  });

  it('숫자나 공백만 길게 이어진 공격용 텍스트도 길이에 비례한 시간 안에 처리함', () => {
    for (const text of ['1'.repeat(100_000), ' '.repeat(100_000)]) {
      // given
      const startedAt = performance.now();

      // when
      const result = DateTimeRemover.remove(text);

      // then
      expect(result).toBe(text);
      expect(performance.now() - startedAt).toBeLessThan(1_000);
    }
  });

  it('연도나 단위가 없어 날짜인지 알 수 없는 숫자는 남김', () => {
    // given
    const text = '가격 11.06달러, 버전 1.2.3, 재고 12개';

    // when
    const result = DateTimeRemover.remove(text);

    // then
    expect(result).toBe(text);
  });
});
