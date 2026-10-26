"use client";

import React from "react";
import Image from "next/image";
import { ExternalLink, History, ImageOff } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { formatDateTime } from "@/lib/dateTime";
import type { WatchSnapshot } from "@/types/tower";

function httpUrl(value: string | null): string | undefined {
  if (value === null || !URL.canParse(value)) return undefined;
  const url = new URL(value);
  return ["http:", "https:"].includes(url.protocol) ? value : undefined;
}

export default function SnapshotTimeline({ snapshots }: { readonly snapshots: readonly WatchSnapshot[] }) {
  const [viewMode, setViewMode] = React.useState<"full" | "diff">("diff");
  return <section className="flex min-w-0 flex-col gap-5">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div><h2 className="text-base font-semibold">변경 타임라인</h2><p className="mt-1 text-sm text-muted-foreground">스냅샷은 한 달간 보관돼요.</p></div>
      <ToggleGroup value={[viewMode]} variant="outline" spacing={0} aria-label="스냅샷 보기 방식" onValueChange={values => { const value = values[0]; if (value === "full" || value === "diff") setViewMode(value); }}>
        <ToggleGroupItem value="diff">변경 부분</ToggleGroupItem><ToggleGroupItem value="full">전체 페이지</ToggleGroupItem>
      </ToggleGroup>
    </div>
    {snapshots.length === 0 && <Empty className="border"><EmptyHeader><EmptyMedia variant="icon"><History /></EmptyMedia><EmptyTitle>아직 기록된 변화가 없어요</EmptyTitle><EmptyDescription>아직 저장된 스냅샷이 없어요.</EmptyDescription></EmptyHeader></Empty>}
    {snapshots.map(snapshot => {
      const image = httpUrl(viewMode === "diff" ? snapshot.screenshotDiffUrl : snapshot.screenshotUrl);
      const html = httpUrl(viewMode === "diff" ? snapshot.htmlContentDiffUrl : snapshot.htmlContentUrl);
      return <Card key={snapshot.id} size="sm">
        <CardHeader><CardTitle><span className="flex items-center gap-2"><History aria-hidden="true" className="size-4 text-primary" />변경 기록</span></CardTitle><CardDescription>{snapshot.notifiedAt ? formatDateTime(snapshot.notifiedAt) : "알림 전 스냅샷"}</CardDescription></CardHeader>
        <CardContent className="flex min-w-0 flex-col gap-4">
          <p className="text-sm leading-relaxed wrap-anywhere">{snapshot.aiDiffSummary || "변경 요약이 없어요."}</p>
          {image ? <a href={image} target="_blank" rel="noopener noreferrer" aria-label="스크린샷 원본 열기">
            <Image src={image} alt={viewMode === "diff" ? "변경 부분 스크린샷" : "전체 페이지 스크린샷"} width={1280} height={720} unoptimized className="h-auto w-full rounded-lg border" />
          </a> : <Empty className="border"><EmptyHeader><EmptyMedia variant="icon"><ImageOff /></EmptyMedia><EmptyDescription>저장된 스크린샷이 없어요.</EmptyDescription></EmptyHeader></Empty>}
        </CardContent>
        {html && <CardFooter><a className={buttonVariants({ variant: "outline", size: "sm" })} href={html} target="_blank" rel="noopener noreferrer">HTML 열기<ExternalLink data-icon="inline-end" /></a></CardFooter>}
      </Card>;
    })}
  </section>;
}
