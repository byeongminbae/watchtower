"use client";

import React from "react";
import { useSearchParams } from "next/navigation";
import { Spinner } from "@/components/ui/spinner";
import { LoaderCircle, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import LighthouseMark from "@/components/common/LighthouseMark";
import { useAuth } from "@/lib/AuthContext";
import { DISCORD_LOGIN_PATH } from "@/lib/api/auth";
import { createOAuthTransaction } from "@/lib/oauthTransaction";

function LoginContent() {
  const searchParams = useSearchParams();
  const { isLoggedIn, isInitializing } = useAuth();
  const [loading, setLoading] = React.useState(false);
  const isStarting = React.useRef(false);

  React.useEffect(() => {
    if (!isInitializing && isLoggedIn) globalThis.location.replace("/");
  }, [isInitializing, isLoggedIn]);

  const handleDiscordLogin = () => {
    if (isStarting.current) return;

    isStarting.current = true;
    setLoading(true);
    createOAuthTransaction(searchParams.get("next") ?? "/");
    globalThis.location.assign(DISCORD_LOGIN_PATH);
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
          <CardHeader><LighthouseMark size={28} /><CardTitle>로그인</CardTitle><CardDescription>Discord 계정으로 간편하게 로그인해요.</CardDescription></CardHeader>
          <CardContent className="flex flex-col gap-4">
            <Button variant="outline" className="w-full" onClick={handleDiscordLogin} disabled={loading}>
              {loading && <LoaderCircle data-icon="inline-start" className="animate-spin motion-reduce:animate-none" />}
              {loading ? "Discord로 연결하고 있어요" : "Discord로 시작하기"}
            </Button>
            <p className="text-center text-xs text-muted-foreground">로그인하면 Watchtower Discord 서버에 참여하고<br />페이지 변화 알림을 Discord DM으로 받아요.</p>
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
