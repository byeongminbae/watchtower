"use client";

import { useSyncExternalStore } from "react";
import { Bell, CircleCheck, Clock3 } from "lucide-react";
import { Card, CardAction, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { formatDateTime, formatRelativeTime, toDate } from "@/lib/dateTime";
import { formatFetchInterval, intervalMinutes } from "@/lib/tower";
import type { Watch } from "@/types/tower";

function subscribeClock(onChange: () => void) {
  const timer = window.setInterval(onChange, 30_000);
  return () => window.clearInterval(timer);
}

function currentTime() {
  return Math.floor(Date.now() / 30_000) * 30_000;
}

function RecentTime({ value, now, emptyText }: { readonly value: string | null; readonly now: number | null; readonly emptyText: string }) {
  if (value === null) return <span className="text-muted-foreground">{emptyText}</span>;
  return <time dateTime={value} title={formatDateTime(value)}>{now === null ? formatDateTime(value) : formatRelativeTime(value, now)}</time>;
}

/** 확인 주기와 다음 확인까지의 진행, 최근 확인과 최근 알림 시각을 지표 카드로 보여줌 */
export default function WatchActivity({ watch }: { readonly watch: Watch }) {
  const now = useSyncExternalStore<number | null>(subscribeClock, currentTime, () => null);
  const interval = intervalMinutes(watch.fetchInterval) * 60_000;
  const elapsed = watch.lastFetchedAt && now !== null ? now - toDate(watch.lastFetchedAt).getTime() : null;
  const running = watch.status === "CRAWLING_READY" || watch.status === "CRAWLING";
  const progress = running && elapsed !== null && Number.isFinite(interval) && interval > 0 ? Math.min(1, Math.max(0, elapsed / interval)) : 0;
  const remaining = elapsed === null ? null : Math.max(0, Math.ceil((interval - elapsed) / 60_000));
  const checkMessage = watch.status === "CRAWLING" ? "지금 페이지를 확인하고 있어요"
    : watch.status === "CRAWLING_ERROR" ? "확인에 실패해서 감시가 멈춰 있어요"
    : !running ? "감시가 멈춰 있어요"
    : remaining === null ? "첫 확인을 기다리고 있어요" : remaining > 0 ? `다음 확인까지 약 ${remaining}분` : "다음 확인을 기다리고 있어요";

  return <section aria-label="감시 활동" className="grid grid-cols-2 gap-4 sm:grid-cols-3">
    <Card className="col-span-2 sm:col-span-1">
      <CardHeader>
        <CardDescription>확인 주기</CardDescription>
        <CardTitle className="text-2xl font-semibold tabular-nums">{formatFetchInterval(watch.fetchInterval)}<span className="ml-1 text-sm font-normal text-muted-foreground">마다</span></CardTitle>
        <CardAction><Clock3 className="text-muted-foreground" aria-hidden="true" /></CardAction>
      </CardHeader>
      <CardFooter className="flex-col items-start gap-2">
        <p className="text-xs text-muted-foreground">{checkMessage}</p>
        <Progress value={progress * 100} aria-label="다음 확인까지 진행" className="w-full" />
      </CardFooter>
    </Card>
    <Card>
      <CardHeader>
        <CardDescription>최근 확인</CardDescription>
        <CardTitle className="text-2xl font-semibold"><RecentTime value={watch.lastFetchedAt} now={now} emptyText="대기 중" /></CardTitle>
        <CardAction><CircleCheck className="text-muted-foreground" aria-hidden="true" /></CardAction>
      </CardHeader>
      <CardFooter><p className="text-xs text-muted-foreground">{watch.lastFetchedAt ? formatDateTime(watch.lastFetchedAt) : "첫 확인을 기다리고 있어요"}</p></CardFooter>
    </Card>
    <Card>
      <CardHeader>
        <CardDescription>최근 알림</CardDescription>
        <CardTitle className="text-2xl font-semibold"><RecentTime value={watch.lastNotifiedAt} now={now} emptyText="없음" /></CardTitle>
        <CardAction><Bell className="text-muted-foreground" aria-hidden="true" /></CardAction>
      </CardHeader>
      <CardFooter><p className="text-xs text-muted-foreground">{watch.lastNotifiedAt ? formatDateTime(watch.lastNotifiedAt) : "아직 보낸 알림이 없어요"}</p></CardFooter>
    </Card>
  </section>;
}
