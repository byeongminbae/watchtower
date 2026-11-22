"use client";

import { LogIn } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import Link from "next/link";
import { useRouter } from "next/navigation";
import React from "react";
import LighthouseMark from "@/components/common/LighthouseMark";
import { useAuth } from "@/lib/AuthContext";
import { authApi } from "@/lib/api";
import {
  clearOAuthTransaction,
  consumeOAuthTransaction,
} from "@/lib/oauthTransaction";

type CallbackStatus =
  | { readonly kind: "loading" }
  | { readonly kind: "success" }
  | { readonly kind: "failure"; readonly errorCode: string | null };

function captureAndCleanErrorCode(): string | null {
  const errorCode = new URLSearchParams(globalThis.location.search).get("error");
  globalThis.history.replaceState(
    globalThis.history.state,
    "",
    "/login/callback",
  );
  return errorCode;
}

function failureMessageFor(errorCode: string | null): string {
  switch (errorCode) {
    case "access_denied":
      return "Discord 로그인을 취소했거나 거부했어요.";
    case "authorization_request_not_found":
      return "로그인 요청이 유효하지 않거나 만료됐어요.";
    case "AUT_006":
      return "탈퇴한 계정이에요.";
    case "AUT_007":
      return "이용이 정지된 계정이에요.";
    case "IDP_002":
      return "다른 회원에 연결된 Discord 계정이에요.";
    case "IDP_003":
      return "이미 Discord 계정이 연결되어 있어요.";
    default:
      return "로그인을 완료할 수 없어요. 다시\u00a0시도해 주세요.";
  }
}

function messageFor(status: CallbackStatus): string {
  switch (status.kind) {
    case "loading":
      return "Discord 로그인을 확인하고 있어요.";
    case "success":
      return "로그인했어요. 이동하고 있어요.";
    case "failure":
      return failureMessageFor(status.errorCode);
    default: {
      const exhaustive: never = status;
      return exhaustive;
    }
  }
}

export default function OAuthCallbackPage(): React.JSX.Element {
  const router = useRouter();
  const { acceptSession } = useAuth();
  const [errorCode, setErrorCode] = React.useState<string | null | undefined>(undefined);
  const [status, setStatus] = React.useState<CallbackStatus>({ kind: "loading" });
  const captured = React.useRef(false);
  const started = React.useRef(false);
  const mounted = React.useRef(false);

  React.useLayoutEffect(() => {
    if (captured.current) return;
    captured.current = true;
    setErrorCode(captureAndCleanErrorCode());
  }, []);

  React.useEffect(() => {
    mounted.current = true;
    if (errorCode !== undefined && !started.current) {
      started.current = true;
      void Promise.resolve().then(() => {
        if (!mounted.current) return;
        if (errorCode !== null) {
          clearOAuthTransaction();
          setStatus({ kind: "failure", errorCode });
          return;
        }
        const returnPath = consumeOAuthTransaction() ?? "/";
        void authApi.getSession().then(
          (response) => {
            if (!mounted.current) return;
            if (!acceptSession(response.data)) {
              setStatus({ kind: "failure", errorCode: null });
              return;
            }
            setStatus({ kind: "success" });
            router.replace(returnPath);
          },
          () => {
            if (mounted.current) setStatus({ kind: "failure", errorCode: null });
          },
        );
      });
    }
    return () => {
      mounted.current = false;
    };
  }, [acceptSession, errorCode, router]);

  const isProgress = status.kind !== "failure";

  return (
    <main className="flex min-h-[calc(100dvh-4rem)] items-center justify-center px-6 py-12">
      <Card className="w-full max-w-md">
        <CardHeader className="items-center gap-4 text-center">
          <LighthouseMark size={40} />
          <CardTitle><h1>로그인 확인</h1></CardTitle>
          <CardDescription>계정 연결 상태를 확인하고 있어요.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col items-center gap-6 pb-4">
          {isProgress ? <>
            <Spinner role="progressbar" aria-label={messageFor(status)} />
            <p role="status" aria-live="polite" className="text-center text-sm text-muted-foreground">{messageFor(status)}</p>
          </> : <>
            <Alert variant="destructive"><AlertDescription>{messageFor(status)}</AlertDescription></Alert>
            <Link href="/login" className={buttonVariants()}><LogIn data-icon="inline-start" />로그인 다시 시도하기</Link>
          </>}
        </CardContent>
      </Card>
    </main>
  );
}
