"use client";

import React from "react";
import { Member } from "@/types/domain";
import { authApi, memberApi } from "@/lib/api";
import { getJwtFromCookie } from "@/lib/jwtCookie";
import { decodeAccessToken, isTokenExpired } from "@/lib/jwt";

interface AuthContextValue {
  isLoggedIn: boolean;
  isInitializing: boolean;
  member: Member | null;
  logout: () => Promise<void>;
  refreshMember: () => Promise<void>;
}

const AuthContext = React.createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [member, setMember] = React.useState<Member | null>(null);
  const [isInitializing, setIsInitializing] = React.useState(true);

  // 쿠키에 담긴 JWT를 읽어 memberId를 추출하고, 그 프로필을 불러온다.
  // 네이버 로그인 완료 후에는 백엔드가 알아서 쿠키를 심고 원래 페이지로 돌려보내므로,
  // 이 함수는 "지금 쿠키 상태를 신뢰해서 프로필을 동기화"하는 역할만 한다.
  const syncFromCookie = React.useCallback(async () => {
    const jwt = getJwtFromCookie();
    if (!jwt) {
      setMember(null);
      return;
    }
    const payload = decodeAccessToken(jwt);
    if (!payload || isTokenExpired(payload)) {
      setMember(null);
      return;
    }
    try {
      const res = await memberApi.getMember(Number(payload.sub));
      setMember(res.data);
    } catch {
      setMember(null);
    }
  }, []);

  React.useEffect(() => {
    let cancelled = false;

    (async () => {
      await syncFromCookie();
      if (!cancelled) setIsInitializing(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [syncFromCookie]);

  const logout = React.useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 서버 로그아웃 실패해도 클라이언트 상태는 정리한다
    } finally {
      setMember(null);
    }
  }, []);

  const refreshMember = React.useCallback(async () => {
    await syncFromCookie();
  }, [syncFromCookie]);

  const value: AuthContextValue = {
    isLoggedIn: member !== null,
    isInitializing,
    member,
    logout,
    refreshMember,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = React.useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
