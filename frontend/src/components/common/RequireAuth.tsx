"use client";

import React from "react";
import { usePathname, useRouter } from "next/navigation";
import { Box, CircularProgress } from "@mui/material";
import { useAuth } from "@/lib/AuthContext";

// 로그인이 필요한 페이지를 감싸는 가드.
// 세션 복원(isInitializing) 완료 후에도 비로그인 상태면, 원래 가려던 경로를 next 쿼리로 실어 /login 으로 리다이렉트.
export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isLoggedIn, isInitializing } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  React.useEffect(() => {
    if (!isInitializing && !isLoggedIn) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [isInitializing, isLoggedIn, router, pathname]);

  if (isInitializing || !isLoggedIn) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
        <CircularProgress color="secondary" size={28} />
      </Box>
    );
  }

  return <>{children}</>;
}
