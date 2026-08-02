"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { Box, CircularProgress, Container, Typography } from "@mui/material";
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
      <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
        <CircularProgress color="secondary" size={28} />
      </Box>
    );
  }

  if (principal?.role !== "ADMIN") {
    return (
      <Container maxWidth="sm" sx={{ py: 16, textAlign: "center" }}>
        <Typography variant="h6" sx={{ fontWeight: 700 }} gutterBottom>
          접근 권한이 없습니다
        </Typography>
        <Typography variant="body2" color="text.secondary">
          이 페이지는 관리자만 접근할 수 있습니다.
        </Typography>
      </Container>
    );
  }

  return <>{children}</>;
}
