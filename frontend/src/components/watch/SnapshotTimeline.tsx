"use client";

import Image from "next/image";
import { History } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { formatDateTime, formatElapsedTime } from "@/lib/dateTime";
import { useNow } from "@/lib/useNow";
import type { WatchSnapshot } from "@/types/tower";

function ElapsedTime({ value, now }: { readonly value: string; readonly now: number | null }) {
  return <time dateTime={value} title={formatDateTime(value)}>{now === null ? formatDateTime(value) : formatElapsedTime(value, now)}</time>;
}

export default function SnapshotTimeline({ snapshots }: { readonly snapshots: readonly WatchSnapshot[] }) {
  const now = useNow();
  return <section aria-label="변화 기록" className="flex min-w-0 flex-col gap-4">
    <p className="text-sm text-muted-foreground">변화가 감지된 순간을 기록해요. 기록은 한 달간 보관돼요.</p>
    {snapshots.length === 0 && <Empty className="border border-dashed bg-card"><EmptyHeader><EmptyMedia variant="icon"><History /></EmptyMedia><EmptyTitle>아직 기록된 변화가 없어요</EmptyTitle><EmptyDescription>아직 저장된 스냅샷이 없어요.</EmptyDescription></EmptyHeader></Empty>}
    {snapshots.map(snapshot => <Card key={snapshot.id} size="sm">
      <CardHeader><CardTitle><span className="flex items-center gap-2"><History aria-hidden="true" className="size-4 text-primary" /><ElapsedTime value={snapshot.detectedAt} now={now} /></span></CardTitle></CardHeader>
      <CardContent className="flex min-w-0 flex-col gap-4">
        <p className="text-sm leading-relaxed wrap-anywhere">{snapshot.aiDiffSummary || "변경 요약이 없어요."}</p>
        {snapshot.notifiedAt && <p className="text-xs text-muted-foreground"><ElapsedTime value={snapshot.notifiedAt} now={now} /> 알림을 보냈어요</p>}
        {snapshot.highlightedScreenshotUrl && <a href={snapshot.highlightedScreenshotUrl} target="_blank" rel="noopener noreferrer" aria-label="변경 부분 스크린샷 원본 열기">
          <Image src={snapshot.highlightedScreenshotUrl} alt="변경 부분을 표시한 스크린샷" width={1280} height={720} unoptimized className="h-auto w-full rounded-lg border" />
        </a>}
      </CardContent>
    </Card>)}
  </section>;
}
