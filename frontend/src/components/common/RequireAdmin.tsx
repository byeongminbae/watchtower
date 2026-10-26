"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { Spinner } from "@/components/ui/spinner";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { useAuth } from "@/lib/AuthContext";

// /admin 하위 라우트 전용 가드. 로그인 + ADMIN 권한을 모두 확인.
export default function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { isLoggedIn, isInitializing, principal } = useAuth();
  const router = useRouter();

  React.useEffect(() => {
    if (!isInitializing && !isLoggedIn) {
      const returnPath = `${globalThis.location.pathname}${globalThis.location.search}${globalThis.location.hash}`;
      router.replace(`/login?next=${encodeURIComponent(returnPath)}`);
    }
  }, [isInitializing, isLoggedIn, router]);

  if (isInitializing || !isLoggedIn) {
    return (
      <div className="flex justify-center py-20">
        <Spinner role="progressbar" aria-label="로그인 상태를 확인하고 있어요" />
      </div>
    );
  }

  if (principal?.role !== "ADMIN") {
    return (
      <div className="mx-auto w-full max-w-lg px-6 py-20">
        <Alert variant="destructive">
          <AlertTitle>접근 권한이 없습니다</AlertTitle>
          <AlertDescription>이 페이지는 관리자만 접근할 수 있어요.</AlertDescription>
        </Alert>
      </div>
    );
  }

  return <>{children}</>;
}
