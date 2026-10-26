"use client";

import { useSyncExternalStore } from "react";
import { Bell, ChartNoAxesCombined, Check, CircleCheck, Clock3 } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
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

export default function WatchActivity({ watch }: { readonly watch: Watch }) {
  const now = useSyncExternalStore<number | null>(subscribeClock, currentTime, () => null);
  const interval = intervalMinutes(watch.fetchInterval) * 60_000;
  const elapsed = watch.lastFetchedAt && now !== null ? now - toDate(watch.lastFetchedAt).getTime() : null;
  const running = watch.status === "RUNNING";
  const progress = running && elapsed !== null && Number.isFinite(interval) && interval > 0 ? Math.min(1, Math.max(0, elapsed / interval)) : 0;
  const remaining = elapsed === null ? null : Math.max(0, Math.ceil((interval - elapsed) / 60_000));
  const checkMessage = !running ? "감시가 멈춰 있어요" : remaining === null ? "첫 확인을 기다리고 있어요" : remaining > 0 ? `다음 확인까지 약 ${remaining}분` : "다음 확인을 기다리고 있어요";

  return <section aria-label="감시 활동" className="grid gap-6 rounded-lg border p-4 sm:grid-cols-2">
    <div className="flex min-w-0 items-center gap-4">
      <div className="relative flex shrink-0 items-center justify-center" aria-hidden="true">
        <svg viewBox="0 0 48 48" className="size-14 -rotate-90 fill-none">
          <circle cx="24" cy="24" r="21" className="stroke-border" strokeWidth="3" />
          <circle cx="24" cy="24" r="21" className="stroke-primary" strokeWidth="3" strokeLinecap="round" pathLength="100" strokeDasharray="100" strokeDashoffset={100 - progress * 100} />
        </svg>
        <Clock3 className="absolute size-5 text-primary" />
      </div>
      <div className="flex min-w-0 flex-col gap-1.5">
        <p className="font-medium"><span className="text-lg font-semibold tabular-nums">{formatFetchInterval(watch.fetchInterval)}</span><span className="ml-1 text-sm text-muted-foreground">마다 확인</span></p>
        <p className="text-xs text-muted-foreground">{checkMessage}</p>
        <Tooltip>
          <TooltipTrigger render={<span tabIndex={0} className="w-fit" />} aria-label={watch.includeInStat ? "인기 URL 통계에 포함" : "인기 URL 통계에서 제외"}>
            <Badge variant={watch.includeInStat ? "secondary" : "outline"}><ChartNoAxesCombined data-icon="inline-start" />{watch.includeInStat ? "인기 집계 참여" : "인기 집계 제외"}{watch.includeInStat && <Check data-icon="inline-end" />}</Badge>
          </TooltipTrigger>
          <TooltipContent>{watch.includeInStat ? "이 페이지의 변화가 인기 URL 집계에 반영돼요." : "이 페이지의 변화는 인기 URL 집계에 반영되지 않아요."}</TooltipContent>
        </Tooltip>
      </div>
    </div>
    <div className="flex min-w-0 flex-col gap-4">
      <div className="flex items-center gap-3">
        <Avatar><AvatarFallback><CircleCheck aria-hidden="true" /></AvatarFallback></Avatar>
        <div className="min-w-0"><p className="text-xs text-muted-foreground">최근 확인</p>{watch.lastFetchedAt ? <time dateTime={watch.lastFetchedAt} title={formatDateTime(watch.lastFetchedAt)} className="text-sm font-medium">{now === null ? "확인 기록 있음" : formatRelativeTime(watch.lastFetchedAt, now)}</time> : <p className="text-sm">첫 확인 대기</p>}</div>
      </div>
      <div className="flex items-center gap-3">
        <Avatar><AvatarFallback><Bell aria-hidden="true" /></AvatarFallback></Avatar>
        <div className="min-w-0"><p className="text-xs text-muted-foreground">최근 알림</p>{watch.lastNotifiedAt ? <time dateTime={watch.lastNotifiedAt} title={formatDateTime(watch.lastNotifiedAt)} className="text-sm font-medium">{now === null ? "알림 기록 있음" : formatRelativeTime(watch.lastNotifiedAt, now)}</time> : <p className="text-sm">아직 보낸 알림이 없어요</p>}</div>
      </div>
    </div>
  </section>;
}
