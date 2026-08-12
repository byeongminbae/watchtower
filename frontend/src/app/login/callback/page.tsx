"use client";

import LoginIcon from "@mui/icons-material/Login";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import Link from "next/link";
import { useRouter } from "next/navigation";
import React from "react";
import LighthouseMark from "@/components/common/LighthouseMark";
import { useAuth } from "@/lib/AuthContext";
import { authApi } from "@/lib/api";
import {
  clearOAuthTransaction,
  consumeOAuthTransaction,
  sanitizeReturnPath,
} from "@/lib/oauthTransaction";

const OAUTH_STORAGE_KEY = "watchtower_oauth_transaction";

type CallbackQuery = {
  readonly code: string | null;
  readonly state: string | null;
  readonly providerError: boolean;
};

type CallbackStatus =
  | { readonly kind: "loading" }
  | { readonly kind: "success" }
  | { readonly kind: "provider-error" }
  | { readonly kind: "invalid-state" }
  | { readonly kind: "exchange-failure" };

function captureAndCleanQuery(): CallbackQuery {
  const params = new URLSearchParams(globalThis.location.search);
  const query: CallbackQuery = {
    code: params.get("code"),
    state: params.get("state"),
    providerError: params.has("error"),
  };
  globalThis.history.replaceState(
    globalThis.history.state,
    "",
    "/login/callback",
  );
  return query;
}

function captureStoredReturnPath(): string | null {
  const raw = sessionStorage.getItem(OAUTH_STORAGE_KEY);
  if (raw === null) return null;

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    if (error instanceof SyntaxError) return null;
    throw error;
  }

  if (
    typeof parsed !== "object" ||
    parsed === null ||
    !("returnPath" in parsed) ||
    typeof parsed.returnPath !== "string"
  ) {
    return null;
  }
  return parsed.returnPath;
}

function messageFor(status: CallbackStatus): string {
  switch (status.kind) {
    case "loading":
      return "네이버 로그인을 확인하고 있습니다.";
    case "success":
      return "로그인이 완료되었습니다. 이동하고 있습니다.";
    case "provider-error":
      return "네이버 로그인이 취소되었거나 거부되었습니다.";
    case "invalid-state":
      return "로그인 요청이 유효하지 않거나 만료되었습니다.";
    case "exchange-failure":
      return "로그인을 완료할 수 없습니다. 다시\u00a0시도해주세요.";
    default: {
      const exhaustive: never = status;
      return exhaustive;
    }
  }
}

export default function OAuthCallbackPage(): React.JSX.Element {
  const router = useRouter();
  const { acceptSession } = useAuth();
  const [query, setQuery] = React.useState<CallbackQuery | null>(null);
  const [status, setStatus] = React.useState<CallbackStatus>({ kind: "loading" });
  const captured = React.useRef(false);
  const started = React.useRef(false);
  const mounted = React.useRef(false);

  React.useLayoutEffect(() => {
    if (captured.current) return;
    captured.current = true;
    setQuery(captureAndCleanQuery());
  }, []);

  React.useEffect(() => {
    mounted.current = true;
    if (query !== null && !started.current) {
      started.current = true;
      void Promise.resolve().then(() => {
        if (!mounted.current) return;
        if (query.providerError) {
          clearOAuthTransaction();
          setStatus({ kind: "provider-error" });
        } else if (
          query.code === null ||
          query.code.length === 0 ||
          query.state === null ||
          query.state.length === 0
        ) {
          if (query.state === null || query.state.length === 0) {
            clearOAuthTransaction();
          } else {
            consumeOAuthTransaction(query.state);
          }
          setStatus({ kind: "invalid-state" });
        } else {
          const storedReturnPath = captureStoredReturnPath();
          const returnPath = consumeOAuthTransaction(query.state);
          if (
            returnPath === null ||
            storedReturnPath === null ||
            sanitizeReturnPath(storedReturnPath) !== storedReturnPath
          ) {
            setStatus({ kind: "invalid-state" });
          } else {
            void authApi.loginWithNaverCallback(query.code, query.state).then(
              (response) => {
                if (!mounted.current) return;
                if (
                  !acceptSession({
                    accessToken: response.data.accessToken,
                    refreshToken: response.data.refreshToken,
                  })
                ) {
                  setStatus({ kind: "exchange-failure" });
                  return;
                }
                setStatus({ kind: "success" });
                router.replace(returnPath);
              },
              () => {
                if (mounted.current) setStatus({ kind: "exchange-failure" });
              },
            );
          }
        }
      });
    }
    return () => {
      mounted.current = false;
    };
  }, [acceptSession, query, router]);

  const isProgress = status.kind === "loading" || status.kind === "success";

  return (
    <Box
      component="main"
      sx={{
        minHeight: "calc(100dvh - 64px)",
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
          <Stack spacing={2.5} alignItems="center">
            <LighthouseMark size={48} />
            <Typography variant="h5" sx={{ fontWeight: 800 }}>
              로그인 확인
            </Typography>
            {isProgress ? (
              <>
                <CircularProgress
                  color="secondary"
                  size={28}
                  aria-label={messageFor(status)}
                />
                <Typography role="status" aria-live="polite" color="text.secondary">
                  {messageFor(status)}
                </Typography>
              </>
            ) : (
              <>
                <Alert
                  severity="error"
                  sx={{
                    width: "100%",
                    textAlign: "left",
                    wordBreak: "keep-all",
                  }}
                >
                  {messageFor(status)}
                </Alert>
                <Button
                  component={Link}
                  href="/login"
                  variant="contained"
                  color="secondary"
                  startIcon={<LoginIcon />}
                  fullWidth
                >
                  로그인 다시 시도하기
                </Button>
              </>
            )}
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
