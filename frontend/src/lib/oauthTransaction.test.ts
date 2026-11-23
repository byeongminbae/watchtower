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

  it("Given a stored transaction, when consumed, then returns its safe path once", () => {
    createOAuthTransaction("/watches?tab=mine");

    const first = consumeOAuthTransaction();
    const replay = consumeOAuthTransaction();

    expect(first).toBe("/watches?tab=mine");
    expect(replay).toBeNull();
  });

  it("Given an unsafe return path, when the transaction is created, then stores the safe fallback", () => {
    createOAuthTransaction("https://attacker.example");

    const result = consumeOAuthTransaction();

    expect(result).toBe("/watches");
  });

  it("Given an expired transaction, when consumed, then rejects it", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-29T00:00:00Z"));
    createOAuthTransaction("/mypage");
    vi.setSystemTime(new Date("2026-07-29T00:11:00Z"));

    const result = consumeOAuthTransaction();

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
