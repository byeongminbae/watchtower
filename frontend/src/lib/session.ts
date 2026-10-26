import type { MemberRole } from "@/types/domain";

export type SessionPrincipal = {
  readonly memberId: number;
  readonly role: MemberRole;
};

let principal: SessionPrincipal | null | undefined;
const listeners = new Set<() => void>();

export function isSessionPrincipal(value: unknown): value is SessionPrincipal {
  return typeof value === "object" && value !== null &&
    "memberId" in value && typeof value.memberId === "number" &&
    Number.isSafeInteger(value.memberId) && value.memberId > 0 &&
    "role" in value && (value.role === "NORMAL" || value.role === "ADMIN");
}

export function commitSession(session: SessionPrincipal): boolean {
  if (!isSessionPrincipal(session)) return false;
  principal = session;
  listeners.forEach(listener => listener());
  return true;
}

export function readSession(): SessionPrincipal | null {
  return principal ?? null;
}

export function readSessionSnapshot(): SessionPrincipal | null | undefined {
  return principal;
}

export function subscribeToSessionChanges(listener: () => void): () => void {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}

export function clearSession(): void {
  principal = null;
  listeners.forEach(listener => listener());
}
