import { readAccessToken } from "@/lib/session";

export function getJwtFromCookie(): string | null {
  return readAccessToken();
}
