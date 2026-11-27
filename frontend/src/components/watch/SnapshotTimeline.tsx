"use client";

import Image from "next/image";
import { History, Maximize2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { watchApi } from "@/lib/api";
import { formatDateTime, formatElapsedTime } from "@/lib/dateTime";
import { useNow } from "@/lib/useNow";
import type { WatchSnapshot } from "@/types/tower";

function ElapsedTime({ value, now }: { readonly value: string; readonly now: number | null }) {
  return <time dateTime={value} title={formatDateTime(value)}>{now === null ? formatDateTime(value) : formatElapsedTime(value, now)}</time>;
}

function HighlightedScreenshot({ url, detectedAt }: { readonly url: string; readonly detectedAt: string }) {
  return <Dialog>
    <DialogTrigger render={<button type="button" aria-label="변경 부분 스크린샷 크게 보기" className="relative block max-h-40 w-fit max-w-full overflow-hidden rounded-lg border sm:max-w-xs" />}>
      <Image src={url} alt="변경 부분을 표시한 스크린샷" width={1280} height={720} unoptimized className="h-auto w-auto max-w-full" />
      <Badge variant="secondary" className="absolute right-2 bottom-2"><Maximize2 /></Badge>
    </DialogTrigger>
    <DialogContent className="sm:max-w-5xl">
      <DialogHeader>
        <DialogTitle>변경 부분 스크린샷</DialogTitle>
        <DialogDescription>{formatDateTime(detectedAt)}에 감지한 변화예요. 새로 생기거나 바뀐 곳을 빨갛게 칠했어요.</DialogDescription>
      </DialogHeader>
      <Image src={url} alt="변경 부분을 표시한 스크린샷 원본" width={1280} height={720} unoptimized className="mx-auto h-auto w-auto max-w-full rounded-lg border" />
    </DialogContent>
  </Dialog>;
}

export default function SnapshotTimeline({ watchId, snapshots }: { readonly watchId: number; readonly snapshots: readonly WatchSnapshot[] }) {
  const now = useNow();
  return <section aria-label="변화 기록" className="flex min-w-0 flex-col gap-4">
    <p className="text-sm text-muted-foreground break-keep">변화가 감지된 순간을 기록해요. 새로 생기거나 바뀐 곳은 스크린샷에 빨갛게 칠해요. 기록은 일주일간 보관돼요.</p>
    {snapshots.length === 0 && <Empty className="border border-dashed bg-card"><EmptyHeader><EmptyMedia variant="icon"><History /></EmptyMedia><EmptyTitle>아직 기록된 변화가 없어요</EmptyTitle><EmptyDescription>아직 저장된 스냅샷이 없어요.</EmptyDescription></EmptyHeader></Empty>}
    {snapshots.map(snapshot => <Card key={snapshot.id} size="sm">
      <CardHeader><CardTitle><span className="flex items-center gap-2"><History aria-hidden="true" className="size-4 text-primary" /><ElapsedTime value={snapshot.detectedAt} now={now} /></span></CardTitle></CardHeader>
      <CardContent className="flex min-w-0 flex-col gap-4">
        <p className="text-sm leading-relaxed wrap-anywhere">{snapshot.aiDiffSummary || "변경 요약이 없어요."}</p>
        {snapshot.notifiedAt && <p className="text-xs text-muted-foreground"><ElapsedTime value={snapshot.notifiedAt} now={now} /> 알림을 보냈어요</p>}
        {snapshot.hasHighlightedScreenshot && <HighlightedScreenshot url={watchApi.highlightedScreenshotUrlOf(watchId, snapshot.id)} detectedAt={snapshot.detectedAt} />}
      </CardContent>
    </Card>)}
  </section>;
}
