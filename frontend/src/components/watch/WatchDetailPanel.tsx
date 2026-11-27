"use client";

import React from "react";
import { CalendarOff, ChartNoAxesCombined, CircleAlert, Edit3, ExternalLink, Globe, Image as ImageIcon, Pause, Play, RotateCw, ShieldBan, Sparkles, SquareDashedMousePointer, Trash2, Type, X } from "lucide-react";
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialogAction, AlertDialogCancel, AlertDialog, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Spinner } from "@/components/ui/spinner";
import type { Watch } from "@/types/tower";
import { watchApi } from "@/lib/api";
import { describeCrawlFailure } from "@/lib/tower";
import { useApiData } from "@/lib/useApiData";
import SnapshotHeatmap from "./SnapshotHeatmap";
import DetectionConditionsDialog from "./DetectionConditionsDialog";
import WatchStats from "./WatchStats";
import EditWatchDialog from "./EditWatchDialog";
import WatchStatusDot from "./WatchStatusDot";

/** 크롤링 중인 타워의 결과가 반영됐는지 다시 확인하는 간격 */
const RUNNING_POLL_MILLIS = 5_000;
/** 크롤링 대기 중인 타워를 서버 스케줄러가 크롤링하기 시작했는지 다시 확인하는 간격 */
const READY_POLL_MILLIS = 30_000;

export default function WatchDetailPanel({ watch, onChanged, onDeleted, headingLevel = 1 }: {
  readonly watch: Watch;
  readonly onChanged?: () => void;
  readonly onDeleted?: () => void;
  readonly headingLevel?: 1 | 2;
}) {
  const conditions = useApiData(() => watchApi.getWatchConditions(watch.id).then(r => r.data), [watch.id]);
  const snapshots = useApiData(() => watchApi.listWatchSnapshots(watch.id).then(r => r.data), [watch.id, watch.lastFetchedAt]);
  const [conditionsOpen, setConditionsOpen] = React.useState(false);
  const [editOpen, setEditOpen] = React.useState(false);
  const [deleteOpen, setDeleteOpen] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const suspended = watch.status === "SUSPENDED";
  const paused = watch.status === "PAUSED";
  const crawlFailed = watch.status === "ERROR";
  const crawling = watch.status === "RUNNING";

  const pollMillis = crawling ? RUNNING_POLL_MILLIS : watch.status === "READY" ? READY_POLL_MILLIS : null;

  // 서버가 상태나 확인 시각을 바꿨는지 이 타워만 조용히 다시 조회하고, 바뀌었을 때만 화면 전체를 새로 불러옴. 탭이 보이지 않으면 조회하지 않음
  React.useEffect(() => {
    if (pollMillis === null || onChanged === undefined) return;
    const timer = window.setInterval(() => {
      if (document.visibilityState !== "visible") return;
      watchApi.getWatch(watch.id)
        .then(response => { if (response.data.status !== watch.status || response.data.lastFetchedAt !== watch.lastFetchedAt) onChanged(); })
        .catch(() => undefined);
    }, pollMillis);
    return () => window.clearInterval(timer);
  }, [pollMillis, watch.id, watch.status, watch.lastFetchedAt, onChanged]);
  const Heading = headingLevel === 1 ? "h1" : "h2";

  async function toggleStatus() {
    if (suspended || busy) return;
    setBusy(true); setError(null);
    try {
      if (paused) await watchApi.resumeWatch(watch.id);
      else await watchApi.pauseWatch(watch.id);
      onChanged?.();
    } catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }
  async function retryCrawl() {
    if (busy) return;
    setBusy(true); setError(null);
    try { await watchApi.retryCrawl(watch.id); onChanged?.(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }
  async function deleteWatch() {
    setBusy(true); setError(null);
    try { await watchApi.deleteWatch(watch.id); setDeleteOpen(false); onDeleted?.(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }
  return <div className="flex min-w-0 flex-col gap-4">
    <Card>
      <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex min-w-0 flex-1 items-start gap-4">
          <Avatar className="size-12"><AvatarImage src={watch.faviconUrl || undefined} alt="" /><AvatarFallback><Globe aria-hidden="true" /></AvatarFallback></Avatar>
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <div className="flex flex-wrap items-center gap-2">
              <CardTitle><Heading className="text-xl font-semibold tracking-tight wrap-anywhere">{watch.name}</Heading></CardTitle>
              <WatchStatusDot status={watch.status} />
            </div>
            <CardDescription><a href={watch.url} target="_blank" rel="noopener noreferrer" className="inline-flex max-w-full items-center gap-1 hover:text-foreground"><span className="truncate">{watch.url}</span><ExternalLink aria-hidden="true" className="size-3 shrink-0" /></a></CardDescription>
            <div className="flex flex-wrap gap-1.5 pt-1">
              {watch.detectTextChange && <Badge variant="outline"><Type data-icon="inline-start" />텍스트 변화 감지</Badge>}
              {watch.detectImageChange && <Badge variant="outline"><ImageIcon data-icon="inline-start" />이미지 변화 감지</Badge>}
              {watch.ignoreDateTime && <Badge variant="outline"><CalendarOff data-icon="inline-start" />날짜와 시간 무시</Badge>}
              {watch.detectionAreaSelector && <Badge variant="outline" className="max-w-full"><SquareDashedMousePointer data-icon="inline-start" /><span className="truncate">영역: {watch.detectionAreaSelector}</span></Badge>}
              <Badge variant="outline"><ChartNoAxesCombined data-icon="inline-start" />{watch.includeInStat ? "인기 집계 참여" : "인기 집계 제외"}</Badge>
            </div>
          </div>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2 sm:justify-end">
          <Button variant="outline" size="sm" disabled={suspended || busy} onClick={() => void toggleStatus()}>
            {busy ? <Spinner data-icon="inline-start" /> : suspended ? <ShieldBan data-icon="inline-start" /> : paused ? <Play data-icon="inline-start" /> : <Pause data-icon="inline-start" />}
            {paused ? "감시 시작" : "감시 일시정지"}
          </Button>
          <Button variant="outline" size="sm" disabled={suspended || busy} onClick={() => setEditOpen(true)}><Edit3 data-icon="inline-start" />타워 수정</Button>
          <Button variant="ghost" size="icon-sm" aria-label="타워 삭제" disabled={busy} onClick={() => setDeleteOpen(true)} className="text-destructive hover:text-destructive"><Trash2 /></Button>
        </div>
      </CardHeader>
      <CardFooter className="@container">
        <WatchStats watch={watch} conditionCount={conditions.data?.length ?? null} onOpenConditions={() => setConditionsOpen(true)} />
      </CardFooter>
    </Card>

    {error && !deleteOpen && <Alert variant="destructive"><CircleAlert /><AlertDescription>{error}</AlertDescription><AlertAction><Button variant="ghost" size="icon-sm" aria-label="오류 닫기" onClick={() => setError(null)}><X /></Button></AlertAction></Alert>}
    {suspended && <Alert variant="destructive"><ShieldBan /><AlertTitle>관리자가 감시를 정지했어요</AlertTitle>
      <AlertDescription>정책 위반 등으로 정지된 타워라 설정을 바꾸거나 다시 시작할 수 없어요.</AlertDescription></Alert>}
    {crawling && <Alert><Spinner /><AlertTitle>페이지를 확인하고 있어요</AlertTitle>
      <AlertDescription>보통 1분 안에 끝나요. 결과가 나오면 이 화면이 자동으로 바뀌어요.</AlertDescription></Alert>}
    {crawlFailed && <Alert variant="destructive"><CircleAlert /><AlertTitle>페이지를 확인하지 못했어요</AlertTitle>
      <AlertDescription>{describeCrawlFailure(watch.lastCrawlerResultCode)}</AlertDescription>
      <div className="col-start-2 mt-2 flex flex-wrap gap-2">
        <Button size="sm" disabled={busy} onClick={() => void retryCrawl()}>{busy ? <Spinner data-icon="inline-start" /> : <RotateCw data-icon="inline-start" />}{busy ? "확인 요청 중" : "다시 확인"}</Button>
        <a href={watch.url} target="_blank" rel="noopener noreferrer" className={buttonVariants({ variant: "outline", size: "sm" })}><ExternalLink data-icon="inline-start" />사이트 열기</a>
      </div></Alert>}

    <SnapshotHeatmap watchId={watch.id} snapshots={snapshots.data} error={snapshots.error} onRetry={snapshots.refetch} />

    {watch.latestAiSummary && <Card size="sm" className="bg-primary/[0.03]">
      <CardHeader>
        <CardDescription className="flex items-center gap-1.5"><Sparkles aria-hidden="true" className="size-3.5" />최근 AI 요약</CardDescription>
        <CardTitle className="text-base leading-relaxed font-normal">{watch.latestAiSummary}</CardTitle>
      </CardHeader>
    </Card>}

    {conditionsOpen && <DetectionConditionsDialog watchId={watch.id} conditions={conditions} onClose={() => setConditionsOpen(false)} onConditionsChanged={() => { conditions.refetch(); onChanged?.(); }} />}
    {editOpen && <EditWatchDialog watch={watch} conditionCount={conditions.data?.length ?? null} onClose={() => setEditOpen(false)} onSaved={() => { setEditOpen(false); onChanged?.(); }} />}
    <AlertDialog open={deleteOpen} onOpenChange={open => { if (!busy) setDeleteOpen(open); }}><AlertDialogContent>
      <AlertDialogHeader><AlertDialogTitle>타워를 삭제할까요?</AlertDialogTitle><AlertDialogDescription>이 타워의 감시를 종료하고 목록에서 삭제해요. 삭제한 타워는 되돌릴 수 없어요.</AlertDialogDescription></AlertDialogHeader>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      <AlertDialogFooter><AlertDialogCancel disabled={busy}>취소</AlertDialogCancel><AlertDialogAction variant="destructive" disabled={busy} onClick={() => void deleteWatch()}>{busy ? "삭제 중" : "삭제"}</AlertDialogAction></AlertDialogFooter>
    </AlertDialogContent></AlertDialog>
  </div>;
}
