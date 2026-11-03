"use client";

import { History } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { formatDateTime } from "@/lib/dateTime";
import type { WatchSnapshot } from "@/types/tower";

export default function SnapshotTimeline({ snapshots }: { readonly snapshots: readonly WatchSnapshot[] }) {
  return <section aria-label="변경 기록" className="flex min-w-0 flex-col gap-4">
    <p className="text-sm text-muted-foreground">변화가 감지된 순간을 기록해요. 기록은 한 달간 보관돼요.</p>
    {snapshots.length === 0 && <Empty className="border border-dashed bg-card"><EmptyHeader><EmptyMedia variant="icon"><History /></EmptyMedia><EmptyTitle>아직 기록된 변화가 없어요</EmptyTitle><EmptyDescription>아직 저장된 스냅샷이 없어요.</EmptyDescription></EmptyHeader></Empty>}
    {snapshots.map(snapshot => <Card key={snapshot.id} size="sm">
      <CardHeader><CardTitle><span className="flex items-center gap-2"><History aria-hidden="true" className="size-4 text-primary" />변경 기록</span></CardTitle><CardDescription>{snapshot.notifiedAt ? formatDateTime(snapshot.notifiedAt) : "알림 전 스냅샷"}</CardDescription></CardHeader>
      <CardContent className="flex min-w-0 flex-col gap-4">
        <p className="text-sm leading-relaxed wrap-anywhere">{snapshot.aiDiffSummary || "변경 요약이 없어요."}</p>
      </CardContent>
    </Card>)}
  </section>;
}
