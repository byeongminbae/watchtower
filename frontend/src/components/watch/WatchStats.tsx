"use client";

import { Bell, ChevronRight, CircleCheck, Clock3, ScanLine } from "lucide-react";
import { formatDateTime, formatRelativeTime } from "@/lib/dateTime";
import { formatFetchInterval, intervalMinutes } from "@/lib/tower";
import { useNow } from "@/lib/useNow";
import type { Watch } from "@/types/tower";
import Stat from "./Stat";

function nextCheckMessageOf(watch: Watch, now: number | null): string {
  if (watch.status === "RUNNING") return "지금 페이지를 확인하고 있어요";
  if (watch.status === "ERROR") return "확인에 실패해서 감시가 멈춰 있어요";
  if (watch.status !== "READY") return "감시가 멈춰 있어요";
  if (watch.lastFetchedAt === null || now === null) return "첫 확인을 기다리고 있어요";
  const elapsedMillis = now - new Date(watch.lastFetchedAt).getTime();
  const remainingMinutes = Math.ceil(((intervalMinutes(watch.fetchInterval) * 60_000) - elapsedMillis) / 60_000);
  return remainingMinutes > 0 ? `다음 확인까지 약 ${remainingMinutes}분` : "다음 확인을 기다리고 있어요";
}

function conditionCountLabelOf(conditionCount: number | null): string {
  if (conditionCount === null) return "-";
  return conditionCount === 0 ? "없음" : `키워드 ${conditionCount}개`;
}

function RecentTime({ value, now, emptyText }: { readonly value: string | null; readonly now: number | null; readonly emptyText: string }) {
  if (value === null) return <span className="text-muted-foreground">{emptyText}</span>;
  return <time dateTime={value} title={formatDateTime(value)}>{now === null ? formatDateTime(value) : formatRelativeTime(value, now)}</time>;
}

export default function WatchStats({ watch, conditionCount, onOpenConditions }: {
  readonly watch: Watch;
  readonly conditionCount: number | null;
  readonly onOpenConditions: () => void;
}) {
  const now = useNow();

  return <dl className="grid w-full grid-cols-2 gap-x-6 gap-y-4 @xl:grid-cols-4">
    <Stat icon={Clock3} label="확인 주기">
      <span title={nextCheckMessageOf(watch, now)}>{formatFetchInterval(watch.fetchInterval)}마다</span>
    </Stat>
    <Stat icon={ScanLine} label="감지 조건">
      <button
        type="button"
        aria-label={`${conditionCountLabelOf(conditionCount)} 감지 조건 관리`}
        onClick={onOpenConditions}
        className="inline-flex max-w-full items-center gap-0.5 rounded-sm underline-offset-4 outline-none hover:underline focus-visible:ring-2 focus-visible:ring-ring"
      >
        <span className="truncate">{conditionCountLabelOf(conditionCount)}</span>
        <ChevronRight aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
      </button>
    </Stat>
    <Stat icon={CircleCheck} label="최근 확인"><RecentTime value={watch.lastFetchedAt} now={now} emptyText="대기 중" /></Stat>
    <Stat icon={Bell} label="최근 알림"><RecentTime value={watch.lastNotifiedAt} now={now} emptyText="없음" /></Stat>
  </dl>;
}
