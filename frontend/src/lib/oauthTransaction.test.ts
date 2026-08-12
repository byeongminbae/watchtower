import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  consumeOAuthTransaction,
  createOAuthTransaction,
  sanitizeReturnPath,
} from "./oauthTransaction";

describe("OAuth transaction", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it("Given two login attempts, when transactions are created, then uses a fresh cryptographic nonce each time", () => {
    vi.spyOn(crypto, "randomUUID")
      .mockReturnValueOnce("11111111-1111-4111-8111-111111111111")
      .mockReturnValueOnce("22222222-2222-4222-8222-222222222222");

    const first = createOAuthTransaction("/watches");
    const second = createOAuthTransaction("/watches");

    expect(first).toBe("11111111-1111-4111-8111-111111111111");
    expect(second).toBe("22222222-2222-4222-8222-222222222222");
  });

  it("Given a stored transaction, when its exact state is consumed, then returns its safe path once", () => {
    vi.spyOn(crypto, "randomUUID").mockReturnValue(
      "11111111-1111-4111-8111-111111111111",
    );
    const state = createOAuthTransaction("/watches?tab=mine");

    const first = consumeOAuthTransaction(state);
    const replay = consumeOAuthTransaction(state);

    expect(first).toBe("/watches?tab=mine");
    expect(replay).toBeNull();
  });

  it("Given a stored transaction, when a different state is consumed, then deletes the transaction", () => {
    vi.spyOn(crypto, "randomUUID").mockReturnValue(
      "11111111-1111-4111-8111-111111111111",
    );
    const state = createOAuthTransaction("/mypage");

    const mismatch = consumeOAuthTransaction("different-state");
    const retry = consumeOAuthTransaction(state);

    expect(mismatch).toBeNull();
    expect(retry).toBeNull();
  });

  it("Given an expired transaction, when its state is consumed, then rejects it", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-29T00:00:00Z"));
    vi.spyOn(crypto, "randomUUID").mockReturnValue(
      "11111111-1111-4111-8111-111111111111",
    );
    const state = createOAuthTransaction("/mypage");
    vi.setSystemTime(new Date("2026-07-29T00:11:00Z"));

    const result = consumeOAuthTransaction(state);

    expect(result).toBeNull();
  });
});

describe("sanitizeReturnPath", () => {
  it("Given a local path with a query, when sanitized, then preserves it", () => {
    expect(sanitizeReturnPath("/watches?tab=mine&sort=recent")).toBe(
      "/watches?tab=mine&sort=recent",
    );
  });

  it.each([
    "https://attacker.example",
    "javascript:alert(1)",
    "//attacker.example/path",
    "\\attacker.example",
    "/safe\\redirect",
    "/%5c%5cattacker.example",
    "/%255c%255cattacker.example",
    "/%2f%2fattacker.example",
    "/watches%00",
    "/watches%250d%250aLocation%253aevil",
    "/watches%ZZ",
    "/watches\nnext",
  ])(
    "Given unsafe return path %j, when sanitized, then falls back to watches",
    (unsafePath) => {
      expect(sanitizeReturnPath(unsafePath)).toBe("/watches");
    },
  );
});
