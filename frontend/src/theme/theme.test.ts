import { describe, expect, it } from "vitest";
import watchtowerTheme from "./theme";

describe("watchtowerTheme", () => {
  it("uses the daylight palette while preserving the lighthouse signal accent", () => {
    // Given: the shared site theme
    // When: its palette is resolved
    const { palette } = watchtowerTheme;

    // Then: every route receives a bright canvas and the existing signal color
    expect(palette.mode).toBe("light");
    expect(palette.background.default).toBe("#F6FAFE");
    expect(palette.secondary.main).toBe("#F4B83A");
  });
});
