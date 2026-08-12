import { unstable_doesMiddlewareMatch } from "next/experimental/testing/server";
import { describe, expect, it } from "vitest";
import { config } from "./proxy";

describe("frontend backend boundary", () => {
  it.each([
    "/api/v1/auth/logout",
    "/api/v1/member/7",
    "/bff/auth/logout",
    "/bff/member/7",
  ])(
    "Given application route %s, when matching middleware, then it cannot use the generic backend proxy",
    (url) => {
      // Given
      // When
      const matches = unstable_doesMiddlewareMatch({ config, nextConfig: {}, url });

      // Then
      expect(matches).toBe(false);
    },
  );

  it("Given the login route, when matching middleware, then session redirect handling remains enabled", () => {
    // Given
    const url = "/login";

    // When
    const matches = unstable_doesMiddlewareMatch({ config, nextConfig: {}, url });

    // Then
    expect(matches).toBe(true);
  });
});
