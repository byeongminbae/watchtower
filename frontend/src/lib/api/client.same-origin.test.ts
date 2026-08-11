import { afterEach, expect, it, vi } from "vitest";

const TIMESTAMP = "2026-08-11T05:50:00";

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllEnvs();
  vi.resetModules();
});

it("Given a legacy public API origin, when the browser calls the API, then it still uses a same-origin path", async () => {
  // Given
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://backend.invalid");
  vi.resetModules();
  const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(
      JSON.stringify({ success: true, data: "ok", timestamp: TIMESTAMP }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );
  const { apiClient } = await import("./client");

  // When
  await apiClient.get("/bff/member/7");

  // Then
  expect(fetchSpy).toHaveBeenCalledWith(
    "/bff/member/7",
    expect.objectContaining({ credentials: "include" }),
  );
});
