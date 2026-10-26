"use client";

import { AlertCircle, RotateCcw } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

export default function ErrorPage({ reset }: { readonly reset: () => void }) {
  return <main className="mx-auto w-full max-w-lg px-6 py-20">
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold tracking-tight">화면을 불러올 수 없어요.</h1>
      <Alert variant="destructive"><AlertCircle /><AlertTitle>연결에 문제가 생겼어요</AlertTitle><AlertDescription>서버 연결 상태를 확인하고 다시 시도해 주세요.</AlertDescription></Alert>
      <Button onClick={reset}><RotateCcw data-icon="inline-start" />다시 시도</Button>
    </div>
  </main>;
}
