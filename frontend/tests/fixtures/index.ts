import { expect, test as base } from "@playwright/test";

type Fixtures = { readonly login: (role?: "NORMAL" | "ADMIN") => Promise<string> };
export const test = base.extend<Fixtures>({
  login: async ({ context }, applyFixture, info) => {
    await applyFixture(async (role = "NORMAL") => {
      const session = `fixture-${role.toLowerCase()}-${info.testId.replaceAll(/[^a-z0-9]/gi, "")}`;
      await context.addCookies([{ name: "JSESSIONID", value: session, url: "http://127.0.0.1:3000", httpOnly: true, sameSite: "Lax" }]);
      return session;
    });
  }
});
export { expect };
