import { describe, expect, it } from "vitest";
import { isDetectionAreaSelectorValid } from "./tower";

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
