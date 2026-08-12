"use client";

import React from "react";
import { useSearchParams } from "next/navigation";
import { Alert, Box, Button, CircularProgress, Container, Paper, Stack, Typography } from "@mui/material";
import LighthouseMark from "@/components/common/LighthouseMark";
import { useAuth } from "@/lib/AuthContext";
import { authApi } from "@/lib/api";
import { ApiError } from "@/lib/api/errors";
import { clearOAuthTransaction, createOAuthTransaction, sanitizeReturnPath } from "@/lib/oauthTransaction";

function LoginContent() {
  const searchParams = useSearchParams();
  const { isLoggedIn, isInitializing } = useAuth();
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const isStarting = React.useRef(false);

  React.useEffect(() => {
    if (!isInitializing && isLoggedIn) globalThis.location.replace("/");
  }, [isInitializing, isLoggedIn]);

  const handleNaverLogin = async () => {
    if (isStarting.current) return;

    isStarting.current = true;
    setLoading(true);
    setError(null);
    try {
      const returnPath = sanitizeReturnPath(searchParams.get("next") ?? "/");
      const nonce = createOAuthTransaction(returnPath);
      const response = await authApi.getNaverLoginUrl(nonce);
      globalThis.location.assign(response.data);
    } catch (err) {
      clearOAuthTransaction();
      setError(err instanceof ApiError ? err.message : "로그인을 시작할 수 없습니다. 잠시 후 다시 시도해주세요.");
      isStarting.current = false;
      setLoading(false);
    }
  };

  if (isInitializing || isLoggedIn) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
        <CircularProgress color="secondary" />
      </Box>
    );
  }

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        display: "flex",
        alignItems: "center",
        background: "var(--wt-login-atmosphere)",
      }}
    >
      <Container maxWidth="xs">
        <Paper
          elevation={0}
          sx={{
            p: { xs: 3, sm: 5 },
            textAlign: "center",
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 4,
          }}
        >
          <Stack spacing={1} alignItems="center" sx={{ mb: 4 }}>
            <LighthouseMark size={48} />
            <Typography variant="h5" sx={{ fontWeight: 800 }}>
              Watchtower
            </Typography>
            <Typography variant="body2" color="text.secondary">
              로그인하고 무료로 변화 알림을 받아보세요!
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
