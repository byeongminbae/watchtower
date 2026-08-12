import { NextRequest } from "next/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DELETE as logout } from "./auth/logout/route";
import { GET as exchangeNaverCallback } from "./auth/naver/callback/route";
import { GET as getNaverUrl } from "./auth/naver/url/route";
import { POST as renewSession } from "./auth/renew/route";
import { GET as getMember } from "./member/[memberId]/route";
import { BACKEND_REQUEST_TIMEOUT_MS } from "@/lib/server/backendGateway";

const TIMESTAMP = "2026-08-12T00:00:00Z";

function backendResponse(): Response {
  return new Response(
    JSON.stringify({ success: true, data: "fixture", timestamp: TIMESTAMP }),
    {
      status: 201,
      headers: {
        "Content-Type": "application/json",
        "Set-Cookie": "backend-cookie=fixture; Path=/; HttpOnly",
        "X-Backend-Fixture": "preserved",
      },
    },
  );
}

function capturedRequest(fetchSpy: ReturnType<typeof vi.spyOn>): Request {
  const call = fetchSpy.mock.calls[0];
  if (call === undefined) throw new Error("백엔드 요청이 기록되지 않았습니다.");
  const [input, init] = call;
  return input instanceof Request ? input : new Request(input, init);
}

describe("Next BFF backend boundary", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllEnvs();
  });

  it.each([
    {
      name: "Naver authorize URL",
      expectedBackendUrl: "http://back-dev:8080/api/v1/auth/naver/url?state=state+fixture",
      invoke: () =>
        getNaverUrl(
          new NextRequest(
            "https://dev.watchtower.boo/bff/auth/naver/url?state=state%20fixture",
            {
              headers: {
                authorization: "Bearer access.fixture",
                cookie: "watchtower_jwt=access.fixture",
              },
            },
          ),
        ),
    },
    {
      name: "Naver callback exchange",
      expectedBackendUrl:
        "http://back-dev:8080/api/v1/auth/naver/callback?code=code+fixture&state=state+fixture",
      invoke: () =>
        exchangeNaverCallback(
          new NextRequest(
            "https://dev.watchtower.boo/bff/auth/naver/callback?code=code%20fixture&state=state%20fixture",
          ),
        ),
    },
    {
      name: "logout",
      expectedBackendUrl: "http://back-dev:8080/api/v1/auth/logout",
      invoke: () =>
        logout(
          new NextRequest("https://dev.watchtower.boo/bff/auth/logout", {
            method: "DELETE",
            headers: { authorization: "Bearer access.fixture" },
          }),
        ),
    },
  ])(
    "Given a browser $name request, when Next handles it, then only the server calls its backend route",
    async ({ expectedBackendUrl, invoke }) => {
      // Given
      vi.stubEnv("INTERNAL_API_BASE_URL", "http://back-dev:8080");
      const fetchSpy = vi
        .spyOn(globalThis, "fetch")
        .mockResolvedValue(backendResponse());

      // When
      const response = await invoke();
      const backendRequest = capturedRequest(fetchSpy);

      // Then
      expect(backendRequest.url).toBe(expectedBackendUrl);
      expect(response.status).toBe(201);
      expect(response.headers.get("x-backend-fixture")).toBe("preserved");
      expect(response.headers.get("set-cookie")).toContain("backend-cookie=fixture");
      await expect(response.json()).resolves.toMatchObject({ success: true });
    },
  );

  it("Given a refresh token body, when Next renews the session, then the backend query stays server-side", async () => {
    // Given
    vi.stubEnv("INTERNAL_API_BASE_URL", "http://back-dev:8080");
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(backendResponse());
    const request = new NextRequest("https://dev.watchtower.boo/bff/auth/renew", {
      method: "POST",
      headers: {
        authorization: "Bearer access.fixture",
        "content-type": "application/json",
      },
      body: JSON.stringify({ refreshToken: "refresh token/fixture" }),
    });

    // When
    await renewSession(request);
    const backendRequest = capturedRequest(fetchSpy);

    // Then
    expect(backendRequest.url).toBe(
      "http://back-dev:8080/api/v1/auth/renew?refreshToken=refresh+token%2Ffixture",
    );
    expect(backendRequest.method).toBe("POST");
    await expect(backendRequest.text()).resolves.toBe("");
  });

  it("Given a member route, when Next loads the profile, then the browser path is mapped to the backend member path", async () => {
    // Given
    vi.stubEnv("INTERNAL_API_BASE_URL", "http://back-dev:8080");
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(backendResponse());
    const request = new NextRequest("https://dev.watchtower.boo/bff/member/7", {
      headers: { authorization: "Bearer access.fixture" },
    });

    // When
    await getMember(request, { params: Promise.resolve({ memberId: "7" }) });
    const backendRequest = capturedRequest(fetchSpy);

    // Then
    expect(backendRequest.url).toBe("http://back-dev:8080/api/v1/member/7");
    expect(backendRequest.headers.get("authorization")).toBe(
      "Bearer access.fixture",
    );
  });

  it("Given no internal backend origin, when a BFF route is requested, then it fails closed without fetch", async () => {
    // Given
    vi.stubEnv("INTERNAL_API_BASE_URL", "");
    const fetchSpy = vi.spyOn(globalThis, "fetch");

    // When
    const response = await logout(
      new NextRequest("https://dev.watchtower.boo/bff/auth/logout", {
        method: "DELETE",
      }),
    );

    // Then
    expect(response.status).toBe(503);
    expect(fetchSpy).not.toHaveBeenCalled();
    await expect(response.json()).resolves.toMatchObject({
      success: false,
      statusCode: "FRONTEND_BACKEND_UNAVAILABLE",
    });
  });

  it("Given a stalled backend, when the server timeout expires, then Next returns a gateway error", async () => {
    // Given
    vi.useFakeTimers();
    vi.stubEnv("INTERNAL_API_BASE_URL", "http://back-dev:8080");
    vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
      const backendRequest =
        input instanceof Request ? input : new Request(input, init);

      return new Promise((_resolve, reject) => {
        backendRequest.signal.addEventListener(
          "abort",
          () => reject(backendRequest.signal.reason),
          { once: true },
        );
      });
    });
    const responsePromise = logout(
      new NextRequest("https://dev.watchtower.boo/bff/auth/logout", {
        method: "DELETE",
      }),
    );

    // When
    await vi.advanceTimersByTimeAsync(BACKEND_REQUEST_TIMEOUT_MS);
    const response = await responsePromise;

    // Then
    expect(response.status).toBe(502);
    await expect(response.json()).resolves.toMatchObject({
      success: false,
      statusCode: "FRONTEND_BACKEND_REQUEST_FAILED",
    });
  });
});
