"use client";

import React from "react";
import { memberApi } from "@/lib/api";
import { ApiError, BackendFeatureUnavailableError } from "@/lib/api/errors";
import { InvalidApiDataError } from "@/lib/api/types";
import {
  clearSession,
  commitSession,
  INVALID_SESSION_SNAPSHOT,
  readSessionSnapshot,
  readSession,
  subscribeToSessionChanges,
  type SessionPrincipal,
} from "@/lib/session";
import type { Member } from "@/types/domain";

export type SessionTokens = {
  readonly accessToken: string;
  readonly refreshToken: string;
};

export type MemberProfileStatus =
  | "idle"
  | "loading"
  | "ready"
  | "unavailable"
  | "error";

type AuthContextValue = {
  readonly isLoggedIn: boolean;
  readonly isInitializing: boolean;
  readonly principal: SessionPrincipal | null;
  readonly member: Member | null;
  readonly memberStatus: MemberProfileStatus;
  readonly acceptSession: (tokens: SessionTokens) => boolean;
  readonly logout: () => void;
  readonly refreshMember: () => Promise<void>;
};

type MemberLoadResult =
  | { readonly kind: "ready"; readonly member: Member }
  | { readonly kind: "unavailable" }
  | { readonly kind: "error" };

const AuthContext = React.createContext<AuthContextValue | null>(null);

function readServerSessionSnapshot(): null {
  return null;
}

function isMember(value: unknown): value is Member {
  if (
    typeof value !== "object" ||
    value === null ||
    Array.isArray(value) ||
    !("id" in value) ||
    !("nickname" in value) ||
    !("email" in value) ||
    !("profileImageUrl" in value) ||
    !("role" in value) ||
    !("lastLoginAt" in value)
  ) {
    return false;
  }

  return (
    typeof value.id === "number" &&
    Number.isSafeInteger(value.id) &&
    value.id > 0 &&
    typeof value.nickname === "string" &&
    typeof value.email === "string" &&
    typeof value.profileImageUrl === "string" &&
    (value.role === "USER" || value.role === "ADMIN") &&
    typeof value.lastLoginAt === "string" &&
    (!("isBanned" in value) || typeof value.isBanned === "boolean")
  );
}

async function fetchMemberProfile(target: SessionPrincipal): Promise<MemberLoadResult> {
  try {
    const response = await memberApi.getMember(target.memberId);
    if (!isMember(response.data) || response.data.id !== target.memberId) {
      return { kind: "unavailable" };
    }
    return { kind: "ready", member: response.data };
  } catch (error) {
    if (
      error instanceof BackendFeatureUnavailableError ||
      error instanceof InvalidApiDataError
    ) {
      return { kind: "unavailable" };
    }
    if (error instanceof ApiError) return { kind: "error" };
    throw error;
  }
}

export function AuthProvider({
  children,
}: {
  readonly children: React.ReactNode;
}): React.JSX.Element {
  const [member, setMember] = React.useState<Member | null>(null);
  const [memberStatus, setMemberStatus] = React.useState<MemberProfileStatus>("idle");
  const profileRequestId = React.useRef(0);

  const clearProfileState = React.useCallback((): void => {
    profileRequestId.current += 1;
    setMember(null);
    setMemberStatus("idle");
  }, []);

  const subscribeToSession = React.useCallback(
    (listener: () => void): (() => void) =>
      subscribeToSessionChanges(() => {
        clearProfileState();
        listener();
      }),
    [clearProfileState],
  );

  const sessionSnapshot = React.useSyncExternalStore(
    subscribeToSession,
    readSessionSnapshot,
    readServerSessionSnapshot,
  );
  const principal = React.useMemo(
    () =>
      sessionSnapshot === null ||
      sessionSnapshot === "" ||
      sessionSnapshot === INVALID_SESSION_SNAPSHOT
        ? null
        : readSession(),
    [sessionSnapshot],
  );

  React.useEffect(() => {
    if (sessionSnapshot === INVALID_SESSION_SNAPSHOT) clearSession();
  }, [sessionSnapshot]);

  const applyMemberResult = React.useCallback((result: MemberLoadResult): void => {
    switch (result.kind) {
      case "ready":
        setMember(result.member);
        setMemberStatus("ready");
        return;
      case "unavailable":
        setMember(null);
        setMemberStatus("unavailable");
        return;
      case "error":
        setMember(null);
        setMemberStatus("error");
        return;
      default: {
        const exhaustive: never = result;
        return exhaustive;
      }
    }
  }, []);

  const refreshMember = React.useCallback(async (): Promise<void> => {
    if (principal === null) {
      profileRequestId.current += 1;
      setMember(null);
      setMemberStatus("idle");
      return;
    }
    setMemberStatus("loading");
    const requestId = profileRequestId.current + 1;
    profileRequestId.current = requestId;
    const result = await fetchMemberProfile(principal);
    if (profileRequestId.current === requestId) applyMemberResult(result);
  }, [applyMemberResult, principal]);

  React.useEffect(() => {
    const requestId = profileRequestId.current + 1;
    profileRequestId.current = requestId;
    if (principal !== null) {
      void fetchMemberProfile(principal).then((result) => {
        if (profileRequestId.current === requestId) applyMemberResult(result);
      });
    }
    return () => {
      profileRequestId.current += 1;
    };
  }, [applyMemberResult, principal]);

  const acceptSession = React.useCallback((tokens: SessionTokens): boolean => {
    if (!commitSession(tokens.accessToken, tokens.refreshToken)) {
      clearProfileState();
      return false;
    }

    const acceptedPrincipal = readSession();
    if (acceptedPrincipal === null) return false;

    profileRequestId.current += 1;
    setMember(null);
    setMemberStatus("loading");
    return true;
  }, [clearProfileState]);

  const logout = React.useCallback((): void => {
    clearSession();
    clearProfileState();
  }, [clearProfileState]);

  const value = React.useMemo<AuthContextValue>(
    () => ({
      isLoggedIn: principal !== null,
      isInitializing: sessionSnapshot === null,
      principal,
      member,
      memberStatus,
      acceptSession,
      logout,
      refreshMember,
    }),
    [
      acceptSession,
      logout,
      member,
      memberStatus,
      principal,
      refreshMember,
      sessionSnapshot,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = React.useContext(AuthContext);
  if (context === null) {
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
