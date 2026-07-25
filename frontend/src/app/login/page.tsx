"use client";

import React from "react";
import { useSearchParams } from "next/navigation";
import { Alert, Box, Button, CircularProgress, Container, Paper, Stack, Typography } from "@mui/material";
import LighthouseMark from "@/components/common/LighthouseMark";
import { authApi } from "@/lib/api";
import { ApiError } from "@/lib/api/errors";

function LoginContent() {
  const searchParams = useSearchParams();
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const handleNaverLogin = async () => {
    setLoading(true);
    setError(null);
    try {
      // RequireAuth/RequireAdmin이 원래 가려던 경로를 ?next= 로 실어 보내준다.
      // 없으면 기본값으로 /watches를 사용.
      const redirectUrl = searchParams.get("next") ?? "/watches";
      const res = await authApi.getNaverLoginUrl(redirectUrl);
      window.location.href = res.data.naverLoginUrl;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인을 시작할 수 없습니다. 잠시 후 다시 시도해주세요.");
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        display: "flex",
        alignItems: "center",
        background:
          "radial-gradient(ellipse 60% 50% at 50% 0%, rgba(255,201,74,0.08), transparent)",
      }}
    >
      <Container maxWidth="xs">
        <Paper elevation={0} sx={{ p: 5, textAlign: "center", border: "1px solid rgba(255,255,255,0.06)" }}>
          <Stack spacing={1} alignItems="center" sx={{ mb: 4 }}>
            <LighthouseMark size={48} />
            <Typography variant="h5" sx={{ fontWeight: 800 }}>
              Watchtower
            </Typography>
            <Typography variant="body2" color="text.secondary">
              로그인하고 감시를 시작하세요
            </Typography>
          </Stack>

          {error && (
            <Alert severity="error" sx={{ mb: 2, textAlign: "left" }}>
              {error}
            </Alert>
          )}

          <Button
            fullWidth
            size="large"
            onClick={handleNaverLogin}
            disabled={loading}
            sx={{
              backgroundColor: "#03C75A",
              color: "#fff",
              py: 1.4,
              fontWeight: 700,
              "&:hover": { backgroundColor: "#02b350" },
              "&.Mui-disabled": { backgroundColor: "#03C75A", opacity: 0.6, color: "#fff" },
            }}
          >
            {loading ? <CircularProgress size={22} sx={{ color: "#fff" }} /> : "네이버로 시작하기"}
          </Button>

          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 3 }}>
            현재는 네이버 로그인만 지원합니다.
          </Typography>
        </Paper>
      </Container>
    </Box>
  );
}

export default function LoginPage() {
  return (
    <React.Suspense
      fallback={
        <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
          <CircularProgress color="secondary" />
        </Box>
      }
    >
      <LoginContent />
    </React.Suspense>
  );
}
