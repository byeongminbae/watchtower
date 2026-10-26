"use client";

import React from "react";
import { useSearchParams } from "next/navigation";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import { LoaderCircle, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
      <div className="flex justify-center py-40"><Spinner aria-label="로그인 상태를 확인하고 있어요" /></div>
    );
  }

  return (
    <main className="flex min-h-[calc(100dvh-4rem)] items-center justify-center px-6 py-12">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <h1 className="text-center text-2xl font-semibold tracking-tight">Watchtower 시작하기</h1>
        <Card>
          <CardHeader><LighthouseMark size={28} /><CardTitle>로그인</CardTitle><CardDescription>네이버 계정으로 간편하게 로그인하세요.</CardDescription></CardHeader>
          <CardContent className="flex flex-col gap-4">
            {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
            <Button variant="outline" className="w-full" onClick={handleNaverLogin} disabled={loading}>
              {loading ? <LoaderCircle data-icon="inline-start" className="animate-spin motion-reduce:animate-none" /> : <span aria-hidden="true" className="font-bold">N</span>}
              {loading ? "네이버로 연결하고 있어요" : "네이버로 시작하기"}
            </Button>
            <p className="text-center text-xs text-muted-foreground">별도 비밀번호 없이 안전하게 로그인해요.<br />현재는 네이버 로그인만 지원해요.</p>
          </CardContent>
        </Card>
        <p className="flex items-center justify-center gap-2 text-xs text-muted-foreground"><ShieldCheck className="size-4" aria-hidden="true" />계정 정보는 안전하게 보호해요</p>
      </div>
    </main>
  );
}

export default function LoginPage() {
  return (
    <React.Suspense
      fallback={
        <div className="flex justify-center py-40"><Spinner aria-label="로그인 화면을 불러오고 있어요" /></div>
      }
    >
      <LoginContent />
    </React.Suspense>
  );
}
