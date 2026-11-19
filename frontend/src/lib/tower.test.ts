import { describe, expect, it } from "vitest";
import { describeCrawlFailure, isDetectionAreaSelectorValid } from "./tower";

describe("감시 영역 선택자 검사", () => {
  it("쉼표로 묶은 여러 선택자는 크롤러가 찾을 수 있으므로 올바른 값으로 볼 것이다", () => {
    // given
    const selector = ".price, #stock > li";
    // when
    const result = isDetectionAreaSelectorValid(selector);
    // then
    expect(result).toBe(true);
  });

  it("문법이 틀린 선택자는 크롤러가 실패하므로 저장 전에 거부할 것이다", () => {
    // given
    const selector = ".price,";
    // when
    const result = isDetectionAreaSelectorValid(selector);
    // then
    expect(result).toBe(false);
  });

  it("비워 둔 영역은 페이지 전체를 감시하므로 올바른 값으로 볼 것이다", () => {
    // when
    const result = isDetectionAreaSelectorValid(null);
    // then
    expect(result).toBe(true);
  });
});

describe("크롤링 실패 안내", () => {
  it("글자가 너무 많아 실패하면 감시할 영역으로 범위를 줄이라고 안내할 것이다", () => {
    // when
    const result = describeCrawlFailure("TEXT_TOO_LARGE");
    // then
    expect(result).toBe("확인할 글자가 너무 많아요. 타워 수정에서 감시할 영역을 지정해 범위를 줄여 주세요.");
  });

  it("실패 이유를 모르면 알 수 없는 결과와 같은 일반 안내를 보여줄 것이다", () => {
    // when
    const results = [describeCrawlFailure(null), describeCrawlFailure("UNKNOWN")];
    // then
    expect(results[0]).toBe(results[1]);
  });
});
