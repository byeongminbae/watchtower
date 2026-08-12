import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearSession, subscribeToSessionChanges } from "@/lib/session";

const NOW_SECONDS = 2_000_000_000;

function createJwt(payload: object): string {
  const encode = (value: object): string =>
    btoa(JSON.stringify(value))
      .replaceAll("+", "-")
      .replaceAll("/", "_")
      .replaceAll("=", "");

  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.fixture`;
}

describe("session lifecycle subscription", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    clearSession();
  });

  afterEach(() => {
    vi.useRealTimers();
    clearSession();
  });

  it("Given cookies changed while a document was frozen, when browser history restores it, then notifies the subscriber", () => {
    // Given
    const listener = vi.fn();
    const unsubscribe = subscribeToSessionChanges(listener);
    const accessToken = createJwt({
      sub: "7",
      role: "NORMAL",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    });
    const refreshToken = createJwt({
      sub: "7",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    });
    document.cookie = `watchtower_jwt=${accessToken}; Path=/`;
    document.cookie = `watchtower_refresh=${refreshToken}; Path=/`;

    // When
    window.dispatchEvent(new Event("pageshow"));

    // Then
    expect(listener).toHaveBeenCalledOnce();
    unsubscribe();
  });

  it("Given cookies did not change while a document was frozen, when browser history restores it, then preserves the subscriber state", () => {
    // Given
    const listener = vi.fn();
    const unsubscribe = subscribeToSessionChanges(listener);

    // When
    window.dispatchEvent(new Event("pageshow"));

    // Then
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });
});
