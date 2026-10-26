"use client";

import React from "react";
import { authApi, memberApi } from "@/lib/api";
import { ApiError } from "@/lib/api/errors";
import {
  clearSession, commitSession, readSessionSnapshot, subscribeToSessionChanges,
  type SessionPrincipal
} from "@/lib/session";
import type { Member } from "@/types/domain";

export type MemberProfileStatus = "idle" | "loading" | "ready" | "unavailable" | "error";
type AuthContextValue = {
  readonly isLoggedIn: boolean;
  readonly isInitializing: boolean;
  readonly principal: SessionPrincipal | null;
  readonly member: Member | null;
  readonly memberStatus: MemberProfileStatus;
  readonly sessionError: string | null;
  readonly acceptSession: (session: SessionPrincipal) => boolean;
  readonly logout: () => Promise<void>;
  readonly refreshMember: () => Promise<void>;
};
const AuthContext = React.createContext<AuthContextValue | null>(null);
const serverSnapshot = () => undefined;

export function AuthProvider({ children }: { readonly children: React.ReactNode }): React.JSX.Element {
  const session = React.useSyncExternalStore(subscribeToSessionChanges, readSessionSnapshot, serverSnapshot);
  const [profile, setProfile] = React.useState<{ readonly owner: number; readonly member: Member } | null>(null);
  const [memberStatus, setMemberStatus] = React.useState<MemberProfileStatus>("idle");
  const [sessionError, setSessionError] = React.useState<string | null>(null);
  const requestId = React.useRef(0);
  const principal = session ?? null;
  const member = profile?.owner === principal?.memberId ? profile?.member ?? null : null;

  React.useEffect(() => {
    let active = true;
    const restore = async () => {
      const previous = readSessionSnapshot();
      try {
        const response = await authApi.getSession();
        if (active && readSessionSnapshot() === previous) { commitSession(response.data); setSessionError(null); }
      } catch (error) {
        if (!(error instanceof ApiError)) throw error;
        if (active && (readSessionSnapshot() === previous || readSessionSnapshot() === null)) {
          clearSession();
          if (error.status !== 401 && !["AUT_001", "AUT_006", "AUT_007", "AUT_008"].includes(error.code ?? "")) {
            setSessionError(error.message);
          }
        }
      }
    };
    void restore();
    window.addEventListener("pageshow", restore);
    return () => { active = false; window.removeEventListener("pageshow", restore); };
  }, []);

  const refreshMember = React.useCallback(async () => {
    const current = ++requestId.current;
    if (principal === null) return;
    setMemberStatus("loading");
    try {
      const response = await memberApi.getMember(principal.memberId);
      if (response.data.id !== principal.memberId) throw new ApiError("요청한 회원과 다른 프로필 정보가 반환되었어요.", 200, "INVALID_RESPONSE");
      if (current === requestId.current) {
        setProfile({ owner: principal.memberId, member: response.data });
        setMemberStatus("ready");
      }
    } catch (error) {
      if (!(error instanceof ApiError)) throw error;
      if (current === requestId.current) { setProfile(null); setMemberStatus("error"); }
    }
  }, [principal]);

  React.useEffect(() => {
    void Promise.resolve().then(refreshMember);
    return () => { requestId.current += 1; };
  }, [refreshMember]);

  const acceptSession = React.useCallback((next: SessionPrincipal) => {
    setSessionError(null);
    return commitSession(next);
  }, []);
  const logout = React.useCallback(async () => {
    await authApi.logout();
    clearSession();
    setProfile(null);
    setMemberStatus("idle");
  }, []);
  const value = React.useMemo<AuthContextValue>(() => ({
    isLoggedIn: principal !== null, isInitializing: session === undefined,
    principal, member, memberStatus, sessionError, acceptSession, logout, refreshMember
  }), [principal, session, member, memberStatus, sessionError, acceptSession, logout, refreshMember]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = React.useContext(AuthContext);
  if (context === null) throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있어요.");
  return context;
}
