import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { decodeAccessToken } from "@/lib/jwt";
import { getJwtFromCookie } from "@/lib/jwtCookie";
import { clearSession, commitSession, readSession } from "@/lib/session";

const NOW_SECONDS = 2_000_000_000;
const ACCESS_COOKIE_NAME = "watchtower_jwt";
const REFRESH_COOKIE_NAME = "watchtower_refresh";

function createJwt(payload: object, signature = "fixture"): string {
  const encode = (value: object): string =>
    btoa(JSON.stringify(value))
      .replaceAll("+", "-")
      .replaceAll("/", "_")
      .replaceAll("=", "");

  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.${signature}`;
}

function createValidTokens(memberId = "1"): readonly [string, string] {
  return [
    createJwt({
      sub: memberId,
      role: "USER",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    }),
    createJwt({ sub: memberId, iat: NOW_SECONDS, exp: NOW_SECONDS + 600 }),
  ];
}

describe("existing JWT utilities", () => {
  beforeEach(() => {
    document.cookie = `${ACCESS_COOKIE_NAME}=; Max-Age=0; Path=/`;
  });

  it("Given encoded JWT text, when decoded, then exposes the current payload", () => {
    // Given
    const token = createJwt({ sub: "7", role: "USER", iat: 1, exp: 2 });

    // When
    const payload = decodeAccessToken(token);

    // Then
    expect(payload).toEqual({ sub: "7", role: "USER", iat: 1, exp: 2 });
  });

});

describe("session cookies", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    clearSession();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    clearSession();
  });

  it("Given a lone valid access cookie, when read through getJwtFromCookie, then clears it and returns null", () => {
    // Given
    const [accessToken] = createValidTokens();
    document.cookie = `${ACCESS_COOKIE_NAME}=${encodeURIComponent(accessToken)}; Path=/`;

    // When
    const storedToken = getJwtFromCookie();

    // Then
    expect(storedToken).toBeNull();
    expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
    expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
  });

  it("Given an expired access cookie and valid refresh cookie, when read through getJwtFromCookie, then clears both and returns null", () => {
    // Given
    const expiredAccessToken = createJwt({
      sub: "1",
      role: "USER",
      iat: NOW_SECONDS - 60,
      exp: NOW_SECONDS,
    });
    const [, refreshToken] = createValidTokens();
    document.cookie = `${ACCESS_COOKIE_NAME}=${encodeURIComponent(expiredAccessToken)}; Path=/`;
    document.cookie = `${REFRESH_COOKIE_NAME}=${encodeURIComponent(refreshToken)}; Path=/`;

    // When
    const storedToken = getJwtFromCookie();

    // Then
    expect(storedToken).toBeNull();
    expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
    expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
  });

  it.each([
    ["missing", null],
    ["malformed", "not-a-jwt"],
  ])("Given a %s refresh cookie, when read through getJwtFromCookie, then clears both and returns null", (_caseName, refreshToken) => {
    // Given
    const [accessToken] = createValidTokens();
    document.cookie = `${ACCESS_COOKIE_NAME}=${encodeURIComponent(accessToken)}; Path=/`;
    if (refreshToken !== null) {
      document.cookie = `${REFRESH_COOKIE_NAME}=${encodeURIComponent(refreshToken)}; Path=/`;
    }

    // When
    const storedToken = getJwtFromCookie();

    // Then
    expect(storedToken).toBeNull();
    expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
    expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
  });

  it("Given a valid access and refresh cookie pair, when read through getJwtFromCookie, then returns the exact access token", () => {
    // Given
    const [accessToken, refreshToken] = createValidTokens();
    document.cookie = `${ACCESS_COOKIE_NAME}=${encodeURIComponent(accessToken)}; Path=/`;
    document.cookie = `${REFRESH_COOKIE_NAME}=${encodeURIComponent(refreshToken)}; Path=/`;

    // When
    const storedToken = getJwtFromCookie();

    // Then
    expect(storedToken).toBe(accessToken);
  });

  it("Given valid access and refresh JWTs, when committed, then reads the minimal principal", () => {
    // Given
    const accessToken = createJwt({ sub: "42", role: "ADMIN", iat: NOW_SECONDS, exp: NOW_SECONDS + 60 });
    const refreshToken = createJwt({
      sub: "42",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    });

    // When
    const committed = commitSession(accessToken, refreshToken);
    const principal = readSession();

    // Then
    expect(committed).toBe(true);
    expect(principal).toEqual({ memberId: 42, role: "ADMIN" });
  });

  it("Given JWTs with different expiries, when committed, then writes independent Max-Age values", () => {
    // Given
    const [accessToken, refreshToken] = createValidTokens();
    const cookieSetter = vi.spyOn(document, "cookie", "set");

    // When
    commitSession(accessToken, refreshToken);

    // Then
    expect(cookieSetter).toHaveBeenCalledWith(
      expect.stringContaining(`${ACCESS_COOKIE_NAME}=${accessToken}; Max-Age=60;`),
    );
    expect(cookieSetter).toHaveBeenCalledWith(
      expect.stringContaining(`${REFRESH_COOKIE_NAME}=${refreshToken}; Max-Age=600;`),
    );
  });

  it("Given an HTTPS browser protocol, when committed, then writes Secure host-only cookies", () => {
    // Given
    const [accessToken, refreshToken] = createValidTokens();
    vi.stubGlobal("location", { protocol: "https:" });
    const cookieSetter = vi.spyOn(document, "cookie", "set");

    // When
    commitSession(accessToken, refreshToken);

    // Then
    expect(cookieSetter).toHaveBeenCalledWith(
      expect.stringMatching(/^watchtower_jwt=.*; Path=\/; SameSite=Lax; Secure$/),
    );
    expect(cookieSetter).toHaveBeenCalledWith(
      expect.stringMatching(/^watchtower_refresh=.*; Path=\/; SameSite=Lax; Secure$/),
    );
    expect(cookieSetter.mock.calls.some(([value]) => value.includes("Domain="))).toBe(false);
  });

  it("Given an HTTP browser protocol, when committed, then omits Secure", () => {
    // Given
    const [accessToken, refreshToken] = createValidTokens();
    vi.stubGlobal("location", { protocol: "http:" });
    const cookieSetter = vi.spyOn(document, "cookie", "set");

    // When
    commitSession(accessToken, refreshToken);

    // Then
    expect(cookieSetter.mock.calls.slice(0, 2).every(([value]) => !value.includes("Secure"))).toBe(
      true,
    );
  });

  it("Given JWT text containing cookie-reserved characters, when committed, then URI encodes it", () => {
    // Given
    const accessToken = createJwt(
      { sub: "5", role: "USER", iat: NOW_SECONDS, exp: NOW_SECONDS + 60 },
      "sig/value?",
    );
    const refreshToken = createJwt(
      { sub: "5", iat: NOW_SECONDS, exp: NOW_SECONDS + 600 },
      "sig/value?",
    );
    const cookieSetter = vi.spyOn(document, "cookie", "set");

    // When
    commitSession(accessToken, refreshToken);

    // Then
    expect(cookieSetter).toHaveBeenCalledWith(expect.stringContaining("sig%2Fvalue%3F"));
    expect(readSession()).toEqual({ memberId: 5, role: "USER" });
  });

  it.each([
    ["bad base64", "x.%%%.x"],
    ["bad JSON", `x.${btoa("{")}.x`],
    ["non-object payload", createJwt([])],
    [
      "invalid role",
      createJwt({ sub: "1", role: "OWNER", iat: NOW_SECONDS, exp: NOW_SECONDS + 60 }),
    ],
    ["zero subject", createJwt({ sub: "0", role: "USER", iat: NOW_SECONDS, exp: NOW_SECONDS + 60 })],
    [
      "non-numeric subject",
      createJwt({ sub: "one", role: "USER", iat: NOW_SECONDS, exp: NOW_SECONDS + 60 }),
    ],
    [
      "fractional subject",
      createJwt({ sub: "1.5", role: "USER", iat: NOW_SECONDS, exp: NOW_SECONDS + 60 }),
    ],
    ["invalid iat", createJwt({ sub: "1", role: "USER", iat: "now", exp: NOW_SECONDS + 60 })],
    ["invalid exp", createJwt({ sub: "1", role: "USER", iat: NOW_SECONDS, exp: "later" })],
    [
      "expired access",
      createJwt({ sub: "1", role: "USER", iat: NOW_SECONDS - 60, exp: NOW_SECONDS }),
    ],
  ])(
    "Given %s access JWT, when committed, then rejects without partial cookies",
    (_caseName, accessToken) => {
      // Given
      document.cookie = `${ACCESS_COOKIE_NAME}=stale; Path=/`;
      document.cookie = `${REFRESH_COOKIE_NAME}=stale; Path=/`;
      const [, refreshToken] = createValidTokens();

      // When
      const committed = commitSession(accessToken, refreshToken);

      // Then
      expect(committed).toBe(false);
      expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
      expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
    },
  );

  it.each([
    ["malformed", "not-a-jwt"],
    ["missing subject", createJwt({ iat: NOW_SECONDS, exp: NOW_SECONDS + 600 })],
    ["invalid subject", createJwt({ sub: -1, iat: NOW_SECONDS, exp: NOW_SECONDS + 600 })],
    ["invalid iat", createJwt({ sub: "1", iat: null, exp: NOW_SECONDS + 600 })],
    ["invalid exp", createJwt({ sub: "1", iat: NOW_SECONDS, exp: null })],
    ["expired", createJwt({ sub: "1", iat: NOW_SECONDS - 600, exp: NOW_SECONDS })],
  ])(
    "Given %s refresh JWT, when committed, then removes both stale cookies",
    (_caseName, refreshToken) => {
      // Given
      document.cookie = `${ACCESS_COOKIE_NAME}=stale; Path=/`;
      document.cookie = `${REFRESH_COOKIE_NAME}=stale; Path=/`;
      const [accessToken] = createValidTokens();

      // When
      const committed = commitSession(accessToken, refreshToken);

      // Then
      expect(committed).toBe(false);
      expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
      expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
    },
  );

  it("Given tokens for different subjects, when committed, then rejects both", () => {
    // Given
    const accessToken = createJwt({
      sub: "1",
      role: "USER",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    });
    const refreshToken = createJwt({
      sub: "2",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    });

    // When
    const committed = commitSession(accessToken, refreshToken);

    // Then
    expect(committed).toBe(false);
    expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
    expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
  });

  it("Given a refresh-cookie write failure, when committed, then rolls back both cookies", () => {
    // Given
    const [accessToken, refreshToken] = createValidTokens();
    const descriptor = Object.getOwnPropertyDescriptor(Document.prototype, "cookie");
    const nativeSetter = descriptor?.set;
    if (nativeSetter === undefined) {
      throw new TypeError("jsdom cookie setter unavailable");
    }
    vi.spyOn(Document.prototype, "cookie", "set").mockImplementation((value) => {
      if (!value.startsWith(`${REFRESH_COOKIE_NAME}=`)) {
        nativeSetter.call(document, value);
      }
    });

    // When
    const committed = commitSession(accessToken, refreshToken);

    // Then
    expect(committed).toBe(false);
    expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
    expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
  });

  it("Given a committed session, when cleared, then removes both cookies", () => {
    // Given
    const [accessToken, refreshToken] = createValidTokens();
    commitSession(accessToken, refreshToken);

    // When
    clearSession();

    // Then
    expect(readSession()).toBeNull();
    expect(document.cookie).not.toContain(`${ACCESS_COOKIE_NAME}=`);
    expect(document.cookie).not.toContain(`${REFRESH_COOKIE_NAME}=`);
  });
});
