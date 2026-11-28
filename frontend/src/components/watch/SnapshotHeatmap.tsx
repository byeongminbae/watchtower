"use client";

import { Fragment, useMemo, useState } from "react";
import Image from "next/image";
import { Bell, BellRing, History, ScanSearch, Sparkles } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { useIsMobile } from "@/hooks/use-mobile";
import { watchApi } from "@/lib/api";
import { formatDateTime } from "@/lib/dateTime";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";
import type { WatchSnapshot } from "@/types/tower";
import Stat from "./Stat";

const RETENTION_DAYS = 7;
const DAY_MS = 86_400_000;
const HOURS_PER_DAY = 24;
const HOURS_PER_SLOT_ON_MOBILE = 3;
const LABELED_HOURS = new Set([0, 6, 12, 18]);
const HEAT_LEVEL_CLASS_NAMES = ["bg-muted", "bg-heatmap-1", "bg-heatmap-2", "bg-heatmap-3", "bg-heatmap-4"] as const;
const WEEKDAY_FORMAT = new Intl.DateTimeFormat("ko-KR", { weekday: "short" });
const CLOCK_TIME_FORMAT = new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
const DETAIL_TIME_FORMAT = new Intl.DateTimeFormat("ko-KR", { month: "long", day: "numeric", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });

function detectedTimeOf(snapshot: WatchSnapshot): number {
  return new Date(snapshot.detectedAt).getTime();
}

function dayStartOf(time: number): Date {
  const date = new Date(time);
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function slotStartOn(dayStart: Date, hour: number): number {
  return new Date(dayStart.getFullYear(), dayStart.getMonth(), dayStart.getDate(), hour).getTime();
}

function slotStartOf(time: number, hoursPerSlot: number): number {
  const hour = new Date(time).getHours();
  return slotStartOn(dayStartOf(time), hour - (hour % hoursPerSlot));
}

function slotStartHoursOf(hoursPerSlot: number): number[] {
  return Array.from({ length: HOURS_PER_DAY / hoursPerSlot }, (_, slotIndex) => slotIndex * hoursPerSlot);
}

function previousDayStartOf(dayStart: Date): Date {
  return new Date(dayStart.getFullYear(), dayStart.getMonth(), dayStart.getDate() - 1);
}

function dayLabelOf(dayStart: Date, now: number): string {
  if (dayStart.getTime() === dayStartOf(now).getTime()) {
    return "오늘";
  }
  return `${dayStart.getMonth() + 1}/${dayStart.getDate()} (${WEEKDAY_FORMAT.format(dayStart)})`;
}

function slotLabelOf(slotStart: number, hoursPerSlot: number, now: number): string {
  const startHour = new Date(slotStart).getHours();
  const hourRange = hoursPerSlot === 1 ? `${startHour}시` : `${startHour}~${startHour + hoursPerSlot}시`;
  return `${dayLabelOf(dayStartOf(slotStart), now)} ${hourRange}`;
}

function snapshotsBySlotStartOf(snapshots: readonly WatchSnapshot[], hoursPerSlot: number): Map<number, WatchSnapshot[]> {
  const snapshotsBySlotStart = new Map<number, WatchSnapshot[]>();
  const oldestFirstSnapshots = [...snapshots].sort((left, right) => detectedTimeOf(left) - detectedTimeOf(right));
  for (const snapshot of oldestFirstSnapshots) {
    const slotStart = slotStartOf(detectedTimeOf(snapshot), hoursPerSlot);
    snapshotsBySlotStart.set(slotStart, [...(snapshotsBySlotStart.get(slotStart) ?? []), snapshot]);
  }
  return snapshotsBySlotStart;
}

function newestFirstDayStartsOf(snapshotsBySlotStart: Map<number, WatchSnapshot[]>, now: number): Date[] {
  const slotStarts = [...snapshotsBySlotStart.keys()];
  const oldestDayStart = dayStartOf(Math.min(now - (RETENTION_DAYS * DAY_MS), ...slotStarts));
  const dayStarts: Date[] = [];
  for (let dayStart = dayStartOf(Math.max(now, ...slotStarts)); dayStart.getTime() >= oldestDayStart.getTime(); dayStart = previousDayStartOf(dayStart)) {
    dayStarts.push(dayStart);
  }
  return dayStarts;
}

function heatLevelClassNameOf(snapshotCount: number, maxSnapshotCount: number): string {
  return HEAT_LEVEL_CLASS_NAMES[Math.ceil((snapshotCount / maxSnapshotCount) * (HEAT_LEVEL_CLASS_NAMES.length - 1))];
}

function HeatCell({ label, slotStart, snapshotCount, maxSnapshotCount, now, isSelected, onSelect }: {
  readonly label: string;
  readonly slotStart: number;
  readonly snapshotCount: number;
  readonly maxSnapshotCount: number;
  readonly now: number;
  readonly isSelected: boolean;
  readonly onSelect: () => void;
}) {
  if (snapshotCount === 0) {
    return <span aria-hidden="true" className={cn("h-8 rounded-sm md:h-6", slotStart <= now && HEAT_LEVEL_CLASS_NAMES[0])} />;
  }
  return <Tooltip>
    <TooltipTrigger render={<button
      type="button"
      aria-label={label}
      aria-pressed={isSelected}
      onClick={onSelect}
      className={cn(
        "h-8 cursor-pointer rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-ring md:h-6",
        "animate-in transition-[background-color,box-shadow] duration-500 fade-in",
        heatLevelClassNameOf(snapshotCount, maxSnapshotCount),
        isSelected && "ring-2 ring-foreground ring-offset-1 ring-offset-background"
      )}
    />} />
    <TooltipContent>{label}</TooltipContent>
  </Tooltip>;
}

function ChangeHeatmap({ snapshotsBySlotStart, hoursPerSlot, now, selectedSlotStart, onSelectSlot }: {
  readonly snapshotsBySlotStart: Map<number, WatchSnapshot[]>;
  readonly hoursPerSlot: number;
  readonly now: number;
  readonly selectedSlotStart: number;
  readonly onSelectSlot: (slotStart: number) => void;
}) {
  const slotStartHours = slotStartHoursOf(hoursPerSlot);
  const maxSnapshotCount = Math.max(...Array.from(snapshotsBySlotStart.values(), slotSnapshots => slotSnapshots.length));

  return <div className="flex flex-col gap-2">
    <TooltipProvider>
      <div className="grid gap-0.5" style={{ gridTemplateColumns: `auto repeat(${slotStartHours.length}, minmax(0, 1fr))` }}>
        <span />
        {slotStartHours.map(hour => <span key={hour} aria-hidden="true" className="text-xs whitespace-nowrap text-muted-foreground">{LABELED_HOURS.has(hour) && `${hour}시`}</span>)}
        {newestFirstDayStartsOf(snapshotsBySlotStart, now).map(dayStart => <Fragment key={dayStart.getTime()}>
          <span className="pr-2 text-xs leading-8 whitespace-nowrap text-muted-foreground md:leading-6">{dayLabelOf(dayStart, now)}</span>
          {slotStartHours.map(hour => {
            const slotStart = slotStartOn(dayStart, hour);
            const snapshotCount = snapshotsBySlotStart.get(slotStart)?.length ?? 0;
            return <HeatCell
              key={hour}
              label={`${slotLabelOf(slotStart, hoursPerSlot, now)} 변화 ${snapshotCount}건`}
              slotStart={slotStart}
              snapshotCount={snapshotCount}
              maxSnapshotCount={maxSnapshotCount}
              now={now}
              isSelected={slotStart === selectedSlotStart}
              onSelect={() => onSelectSlot(slotStart)}
            />;
          })}
        </Fragment>)}
      </div>
    </TooltipProvider>
    <div aria-hidden="true" className="flex items-center justify-end gap-1 text-xs text-muted-foreground">
      적음{HEAT_LEVEL_CLASS_NAMES.map(className => <span key={className} className={cn("size-3 rounded-sm", className)} />)}많음
    </div>
  </div>;
}

function NotificationMark() {
  return <span className="flex size-6 items-center justify-center rounded-full bg-muted text-muted-foreground">
    <BellRing aria-hidden="true" className="size-3.5" /><span className="sr-only">알림을 보냈어요</span>
  </span>;
}

function TimelineDot() {
  return <span aria-hidden="true" className="flex size-6 items-center justify-center"><span className="size-2 rounded-full bg-muted-foreground/40" /></span>;
}

function SnapshotTimelineEntry({ watchId, snapshot, onSelect }: {
  readonly watchId: number;
  readonly snapshot: WatchSnapshot;
  readonly onSelect: (snapshot: WatchSnapshot) => void;
}) {
  return <li className="group flex animate-in gap-2 duration-500 fade-in motion-safe:slide-in-from-top-1">
    <time dateTime={snapshot.detectedAt} title={formatDateTime(snapshot.detectedAt)} className="w-10 shrink-0 py-1 text-xs leading-6 text-muted-foreground tabular-nums">
      {CLOCK_TIME_FORMAT.format(detectedTimeOf(snapshot))}
    </time>
    <div className="flex flex-col items-center pt-1">
      {snapshot.notifiedAt ? <NotificationMark /> : <TimelineDot />}
      <Separator orientation="vertical" className="mt-1 flex-1 group-last:hidden data-vertical:self-center" />
    </div>
    <button
      type="button"
      onClick={() => onSelect(snapshot)}
      className="mb-3 flex min-w-0 flex-1 cursor-pointer items-start gap-3 rounded-md px-2 py-1 text-left outline-none group-last:mb-0 hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring"
    >
      <span className="line-clamp-2 min-w-0 flex-1 text-sm leading-6">
        {snapshot.aiDiffSummary
          ? <><Sparkles aria-hidden="true" className="mr-1 inline size-3.5 align-[-2px] text-muted-foreground" />{snapshot.aiDiffSummary}</>
          : <span className="text-muted-foreground">AI 요약이 없어요.</span>}
      </span>
      {snapshot.hasHighlightedScreenshot && <Image
        src={watchApi.highlightedScreenshotUrlOf(watchId, snapshot.id)}
        alt=""
        width={1280}
        height={720}
        unoptimized
        className="aspect-video w-20 shrink-0 rounded-sm border object-cover object-top"
      />}
    </button>
  </li>;
}

function SlotSnapshotTimeline({ watchId, title, snapshots, onSelectSnapshot }: {
  readonly watchId: number;
  readonly title: string;
  readonly snapshots: readonly WatchSnapshot[];
  readonly onSelectSnapshot: (snapshot: WatchSnapshot) => void;
}) {
  return <section className="flex flex-col gap-3">
    <h3 className="text-sm font-medium">{title}</h3>
    <ol className="flex flex-col">
      {snapshots.map(snapshot => <SnapshotTimelineEntry key={snapshot.id} watchId={watchId} snapshot={snapshot} onSelect={onSelectSnapshot} />)}
    </ol>
  </section>;
}

function ChangeHistory({ watchId, snapshots, now, onSelectSnapshot }: {
  readonly watchId: number;
  readonly snapshots: readonly WatchSnapshot[];
  readonly now: number;
  readonly onSelectSnapshot: (snapshot: WatchSnapshot) => void;
}) {
  const hoursPerSlot = useIsMobile() ? HOURS_PER_SLOT_ON_MOBILE : 1;
  const [selectedSlotStart, setSelectedSlotStart] = useState<number | null>(null);
  const snapshotsBySlotStart = useMemo(() => snapshotsBySlotStartOf(snapshots, hoursPerSlot), [snapshots, hoursPerSlot]);
  const shownSlotStart = (selectedSlotStart !== null && snapshotsBySlotStart.has(selectedSlotStart))
    ? selectedSlotStart
    : Math.max(...snapshotsBySlotStart.keys());
  const shownSlotSnapshots = snapshotsBySlotStart.get(shownSlotStart) ?? [];

  return <div className="flex flex-col gap-4">
    <ChangeHeatmap snapshotsBySlotStart={snapshotsBySlotStart} hoursPerSlot={hoursPerSlot} now={now} selectedSlotStart={shownSlotStart} onSelectSlot={setSelectedSlotStart} />
    <Separator />
    <SlotSnapshotTimeline
      watchId={watchId}
      title={`${slotLabelOf(shownSlotStart, hoursPerSlot, now)} 변화 ${shownSlotSnapshots.length}건`}
      snapshots={shownSlotSnapshots}
      onSelectSnapshot={onSelectSnapshot}
    />
  </div>;
}

function DetailTime({ value }: { readonly value: string }) {
  return <time dateTime={value} title={formatDateTime(value)}>{DETAIL_TIME_FORMAT.format(new Date(value))}</time>;
}

function SnapshotDetails({ snapshot }: { readonly snapshot: WatchSnapshot }) {
  return <div className="flex flex-col gap-4">
    <dl className="grid grid-cols-2 gap-x-6">
      <Stat icon={ScanSearch} label="감지 시간"><DetailTime value={snapshot.detectedAt} /></Stat>
      <Stat icon={Bell} label="알림 시간">
        {snapshot.notifiedAt ? <DetailTime value={snapshot.notifiedAt} /> : <span className="text-muted-foreground">보내지 않았어요</span>}
      </Stat>
    </dl>
    <Alert>
      <Sparkles />
      <AlertTitle>AI 요약</AlertTitle>
      <AlertDescription className="wrap-anywhere">{snapshot.aiDiffSummary || "AI 요약이 없어요."}</AlertDescription>
    </Alert>
  </div>;
}

function SnapshotScreenshot({ watchId, snapshot }: { readonly watchId: number; readonly snapshot: WatchSnapshot }) {
  if (!snapshot.hasHighlightedScreenshot) {
    return <p className="text-sm text-muted-foreground">이 변화는 스크린샷이 없어요.</p>;
  }
  return <figure className="flex flex-col gap-2">
    <figcaption className="text-xs text-muted-foreground">새로 생기거나 바뀐 곳을 빨갛게 칠했어요.</figcaption>
    <Image src={watchApi.highlightedScreenshotUrlOf(watchId, snapshot.id)} alt="변경 부분을 표시한 스크린샷 원본" width={1280} height={720} unoptimized className="mx-auto h-auto w-auto max-w-full rounded-lg border" />
  </figure>;
}

function SnapshotDialog({ watchId, snapshot, onClose }: {
  readonly watchId: number;
  readonly snapshot: WatchSnapshot | null;
  readonly onClose: () => void;
}) {
  return <Dialog open={snapshot !== null} onOpenChange={open => { if (!open) { onClose(); } }}>
    <DialogContent className="sm:max-w-5xl">
      {snapshot && <>
        <DialogHeader>
          <DialogTitle>변화 상세</DialogTitle>
        </DialogHeader>
        <SnapshotDetails snapshot={snapshot} />
        <Separator />
        <SnapshotScreenshot watchId={watchId} snapshot={snapshot} />
      </>}
    </DialogContent>
  </Dialog>;
}

function SnapshotHeatmapBody({ watchId, snapshots, error, onRetry, now, onSelectSnapshot }: {
  readonly watchId: number;
  readonly snapshots: readonly WatchSnapshot[] | null;
  readonly error: string | null;
  readonly onRetry: () => void;
  readonly now: number | null;
  readonly onSelectSnapshot: (snapshot: WatchSnapshot) => void;
}) {
  if (error) {
    return <Alert variant="destructive"><AlertDescription>{error}<Button variant="outline" size="sm" onClick={onRetry}>재시도</Button></AlertDescription></Alert>;
  }
  if (snapshots === null || now === null) {
    return <div role="status"><p className="sr-only">변화 기록 불러오는 중</p><Skeleton className="h-56 w-full" /></div>;
  }
  if (snapshots.length === 0) {
    return <Empty className="border border-dashed"><EmptyHeader><EmptyMedia variant="icon"><History /></EmptyMedia><EmptyTitle>아직 기록된 변화가 없어요</EmptyTitle><EmptyDescription>변화가 감지되면 시간대별로 표시돼요.</EmptyDescription></EmptyHeader></Empty>;
  }
  return <ChangeHistory watchId={watchId} snapshots={snapshots} now={now} onSelectSnapshot={onSelectSnapshot} />;
}

/** @param snapshots 불러오는 중이면 null */
export default function SnapshotHeatmap({ watchId, snapshots, error, onRetry }: {
  readonly watchId: number;
  readonly snapshots: readonly WatchSnapshot[] | null;
  readonly error: string | null;
  readonly onRetry: () => void;
}) {
  const now = useNow();
  const [selectedSnapshot, setSelectedSnapshot] = useState<WatchSnapshot | null>(null);

  return <Card>
    <CardHeader>
      <CardTitle className="flex items-center gap-2">
        <History aria-hidden="true" className="size-4 text-primary" />변화 기록{snapshots && <Badge variant="secondary">{snapshots.length}</Badge>}
      </CardTitle>
      <CardDescription>최근 {RETENTION_DAYS}일간 감지한 변화예요.</CardDescription>
    </CardHeader>
    <CardContent>
      <SnapshotHeatmapBody watchId={watchId} snapshots={snapshots} error={error} onRetry={onRetry} now={now} onSelectSnapshot={setSelectedSnapshot} />
    </CardContent>
    <SnapshotDialog watchId={watchId} snapshot={selectedSnapshot} onClose={() => setSelectedSnapshot(null)} />
  </Card>;
}
