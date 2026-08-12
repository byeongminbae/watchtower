import { describe, expect, it } from "vitest";
import { ApiError, BackendFeatureUnavailableError } from "./errors";

describe("BackendFeatureUnavailableError", () => {
  it("Given a known placeholder contract, when represented, then remains a typed API failure", () => {
    // Given
    const feature = "member.getMember";

    // When
    const error = new BackendFeatureUnavailableError(feature);

    // Then
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      name: "BackendFeatureUnavailableError",
      status: 501,
      code: "BACKEND_FEATURE_UNAVAILABLE",
      feature,
    });
  });
});
