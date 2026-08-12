import { expect, test as base } from "@playwright/test";
import type { Page } from "@playwright/test";

const OAUTH_STORAGE_KEY = "watchtower_oauth_transaction";

type TokenPair = {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly accessTokenExpiry: number;
};

type OAuthHarness = {
  readonly storeTransaction: (
    page: Page,
    state: string,
    returnPath?: string,
  ) => Promise<void>;
  readonly validTokens: (memberId?: number) => TokenPair;
  readonly callbackRequestCount: () => number;
};

type OAuthFixtures = {
  readonly oauth: OAuthHarness;
};

function createJwt(payload: Readonly<Record<string, string | number>>): string {
  const header = Buffer.from(
    JSON.stringify({ alg: "none", typ: "JWT" }),
  ).toString("base64url");
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  return `${header}.${body}.fixture`;
}

export const test = base.extend<OAuthFixtures>({
  oauth: async ({ page }, applyFixture) => {
    let callbackRequests = 0;
    page.on("request", (request) => {
      if (new URL(request.url()).pathname === "/bff/auth/naver/callback") {
        callbackRequests += 1;
      }
    });

    await applyFixture({
      storeTransaction: async (
        target,
        state,
        returnPath = "/watches",
      ): Promise<void> => {
        await target.goto("/login");
        await target.evaluate(
          ({ key, nonce, path, createdAt }) => {
            sessionStorage.setItem(
              key,
              JSON.stringify({ nonce, returnPath: path, createdAt }),
            );
          },
          {
            key: OAUTH_STORAGE_KEY,
            nonce: state,
            path: returnPath,
            createdAt: Date.now(),
          },
        );
      },
      validTokens: (memberId = 7): TokenPair => {
        const now = Math.floor(Date.now() / 1_000);
        return {
          accessToken: createJwt({
            sub: String(memberId),
            role: "NORMAL",
            iat: now,
            exp: now + 3_600,
          }),
          refreshToken: createJwt({
            sub: String(memberId),
            iat: now,
            exp: now + 86_400,
          }),
          accessTokenExpiry: 3_600_000,
        };
      },
      callbackRequestCount: (): number => callbackRequests,
    });
  },
});

export { expect };
