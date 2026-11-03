"use client";

import React from "react";
import { ChartNoAxesCombined, CircleAlert, Edit3, ExternalLink, Globe, History, Pause, Play, Plus, Regex, RotateCw, ScanLine, ShieldBan, Sparkles, TextSearch, Trash2, X } from "lucide-react";
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialogAction, AlertDialogCancel, AlertDialog, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia } from "@/components/ui/empty";
import { Item, ItemActions, ItemContent, ItemDescription, ItemMedia, ItemTitle } from "@/components/ui/item";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Spinner } from "@/components/ui/spinner";
import type { Watch, WatchCondition, WatchConditionInput } from "@/types/tower";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import WatchActivity from "./WatchActivity";
import SnapshotTimeline from "./SnapshotTimeline";
import AddConditionDialog from "./AddConditionDialog";
import EditWatchDialog from "./EditWatchDialog";
import WatchStatusDot from "./WatchStatusDot";

/** 크롤링 중인 타워의 결과가 반영됐는지 다시 확인하는 간격 */
const CRAWLING_POLL_MILLIS = 5_000;

export default function WatchDetailPanel({ watch, onChanged, onDeleted, headingLevel = 1 }: {
  readonly watch: Watch;
  readonly onChanged?: () => void;
  readonly onDeleted?: () => void;
  readonly headingLevel?: 1 | 2;
}) {
  const conditions = useApiData(() => watchApi.getWatchConditions(watch.id).then(r => r.data), [watch.id]);
  const snapshots = useApiData(() => watchApi.listWatchSnapshots(watch.id).then(r => r.data), [watch.id]);
  const [conditionDialog, setConditionDialog] = React.useState<{ readonly initial?: WatchCondition } | null>(null);
  const [editOpen, setEditOpen] = React.useState(false);
  const [deleteOpen, setDeleteOpen] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const suspended = watch.status === "ADMIN_PAUSED";
  const paused = watch.status === "USER_PAUSED";
  const crawlFailed = watch.status === "CRAWLING_ERROR";
  const crawling = watch.status === "CRAWLING";

  // 크롤링 결과가 반영될 때까지 이 타워만 조용히 다시 조회하고, 상태가 바뀌면 그때 한 번만 화면 전체를 새로 불러옴
  React.useEffect(() => {
    if (!crawling || onChanged === undefined) return;
    const timer = window.setInterval(() => {
      watchApi.getWatch(watch.id)
        .then(response => { if (response.data.status !== "CRAWLING") onChanged(); })
        .catch(() => undefined);
    }, CRAWLING_POLL_MILLIS);
    return () => window.clearInterval(timer);
  }, [crawling, watch.id, onChanged]);
  const Heading = headingLevel === 1 ? "h1" : "h2";

  async function toggleStatus() {
    if (suspended || busy) return;
    setBusy(true); setError(null);
    try {
      await watchApi.updateWatch(watch.id, {
        name: watch.name,
        fetchInterval: watch.fetchInterval,
        includeInStat: watch.includeInStat,
        status: paused ? "CRAWLING_READY" : "USER_PAUSED"
      });
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
  async function saveCondition(condition: WatchConditionInput) {
    const initial = conditionDialog?.initial;
    if (initial === undefined) await watchApi.createWatchCondition(watch.id, condition);
    else if (condition.type !== "HTML") await watchApi.updateWatchCondition(watch.id, initial.id, condition);
    conditions.refetch(); onChanged?.();
  }
  async function removeCondition(id: number) {
    setBusy(true); setError(null);
    try { await watchApi.deleteWatchCondition(watch.id, id); conditions.refetch(); onChanged?.(); }
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
    </Card>

    {error && !deleteOpen && <Alert variant="destructive"><CircleAlert /><AlertDescription>{error}</AlertDescription><AlertAction><Button variant="ghost" size="icon-sm" aria-label="오류 닫기" onClick={() => setError(null)}><X /></Button></AlertAction></Alert>}
    {suspended && <Alert variant="destructive"><ShieldBan /><AlertTitle>관리자가 감시를 정지했어요</AlertTitle>
      <AlertDescription>정책 위반 등으로 정지된 타워라 설정을 바꾸거나 다시 시작할 수 없어요.</AlertDescription></Alert>}
    {crawling && <Alert><Spinner /><AlertTitle>페이지를 확인하고 있어요</AlertTitle>
      <AlertDescription>보통 1분 안에 끝나요. 결과가 나오면 이 화면이 자동으로 바뀌어요.</AlertDescription></Alert>}
    {crawlFailed && <Alert variant="destructive"><CircleAlert /><AlertTitle>페이지를 확인하지 못했어요</AlertTitle>
      <AlertDescription>사이트가 응답하지 않거나 접근할 수 없는 페이지예요. 사이트를 직접 열어 주소를 확인한 뒤 다시 확인해 주세요.</AlertDescription>
      <div className="col-start-2 mt-2 flex flex-wrap gap-2">
        <Button size="sm" disabled={busy} onClick={() => void retryCrawl()}>{busy ? <Spinner data-icon="inline-start" /> : <RotateCw data-icon="inline-start" />}{busy ? "확인 요청 중" : "다시 확인"}</Button>
        <a href={watch.url} target="_blank" rel="noopener noreferrer" className={buttonVariants({ variant: "outline", size: "sm" })}><ExternalLink data-icon="inline-start" />사이트 열기</a>
      </div></Alert>}

    <WatchActivity watch={watch} />

    {watch.latestAiSummary && <Card size="sm" className="bg-primary/[0.03]">
      <CardHeader>
        <CardDescription className="flex items-center gap-1.5"><Sparkles aria-hidden="true" className="size-3.5" />최근 변화 요약</CardDescription>
        <CardTitle className="text-base leading-relaxed font-normal">{watch.latestAiSummary}</CardTitle>
      </CardHeader>
    </Card>}

    <Tabs defaultValue="conditions" className="gap-4">
      <TabsList>
        <TabsTrigger value="conditions"><ScanLine data-icon="inline-start" />감지 조건{conditions.data && <Badge variant="secondary">{conditions.data.length}</Badge>}</TabsTrigger>
        <TabsTrigger value="snapshots"><History data-icon="inline-start" />변경 기록{snapshots.data && <Badge variant="secondary">{snapshots.data.length}</Badge>}</TabsTrigger>
      </TabsList>
      <TabsContent value="conditions">
        <Card>
          <CardHeader>
            <CardTitle>감지 조건</CardTitle>
            <CardDescription>이 조건에 맞는 변화가 생기면 알림을 보내요.</CardDescription>
            <CardAction><Button size="sm" disabled={busy} onClick={() => setConditionDialog({})}><Plus data-icon="inline-start" />조건 추가</Button></CardAction>
          </CardHeader>
          <CardContent className="flex min-w-0 flex-col gap-2">
            {conditions.loading && conditions.data === null && <div role="status" className="flex flex-col gap-2"><p className="sr-only">조건 불러오는 중</p><Skeleton className="h-14 w-full" /></div>}
            {conditions.error && <Alert variant="destructive"><AlertDescription>{conditions.error}<Button variant="outline" size="sm" onClick={conditions.refetch}>재시도</Button></AlertDescription></Alert>}
            {!conditions.error && conditions.data?.length === 0 && <Empty className="border border-dashed"><EmptyHeader><EmptyMedia variant="icon"><ScanLine /></EmptyMedia><EmptyDescription>등록된 조건이 없어요. 조건을 추가해 주세요.</EmptyDescription></EmptyHeader></Empty>}
            {!conditions.error && conditions.data?.map(condition => <Item key={condition.id} variant="outline">
              <ItemMedia variant="icon">{condition.type === "HTML" ? <ScanLine /> : condition.type === "KEYWORD" ? <TextSearch /> : <Regex />}</ItemMedia>
              <ItemContent className="min-w-0">
                <ItemTitle className="wrap-anywhere">{condition.type === "HTML" ? "전체 변경 감지" : condition.type === "KEYWORD" ? `키워드: ${condition.keyword}` : `정규식: ${condition.regex}`}</ItemTitle>
                <ItemDescription>{condition.type === "HTML" ? "페이지 내용이 바뀌면 알림" : condition.isSatisfiedAtAppearance ? "등장할 때 알림" : "사라질 때 알림"}</ItemDescription>
              </ItemContent>
              <ItemActions>
                {condition.type !== "HTML" && <Button variant="ghost" size="sm" disabled={busy} onClick={() => setConditionDialog({ initial: condition })}>조건 수정</Button>}
                <Button variant="ghost" size="sm" disabled={busy} className="text-destructive hover:text-destructive" onClick={() => void removeCondition(condition.id)}>조건 삭제</Button>
              </ItemActions>
            </Item>)}
          </CardContent>
        </Card>
      </TabsContent>
      <TabsContent value="snapshots">
        {snapshots.loading && snapshots.data === null && <div role="status"><p className="sr-only">스냅샷 불러오는 중</p><Skeleton className="h-40 w-full" /></div>}
        {snapshots.error && <Alert variant="destructive"><AlertDescription>{snapshots.error}<Button variant="outline" size="sm" onClick={snapshots.refetch}>재시도</Button></AlertDescription></Alert>}
        {!snapshots.error && snapshots.data && <SnapshotTimeline snapshots={snapshots.data} />}
      </TabsContent>
    </Tabs>

    {conditionDialog !== null && <AddConditionDialog open onClose={() => setConditionDialog(null)} onAdd={saveCondition} initial={conditionDialog.initial} hasHtmlCondition={conditions.data?.some(condition => condition.type === "HTML") ?? false} />}
    {editOpen && <EditWatchDialog watch={watch} onClose={() => setEditOpen(false)} onSaved={() => { setEditOpen(false); onChanged?.(); }} />}
    <AlertDialog open={deleteOpen} onOpenChange={open => { if (!busy) setDeleteOpen(open); }}><AlertDialogContent>
      <AlertDialogHeader><AlertDialogTitle>타워를 삭제할까요?</AlertDialogTitle><AlertDialogDescription>이 타워의 감시를 종료하고 목록에서 삭제해요. 삭제한 타워는 되돌릴 수 없어요.</AlertDialogDescription></AlertDialogHeader>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      <AlertDialogFooter><AlertDialogCancel disabled={busy}>취소</AlertDialogCancel><AlertDialogAction variant="destructive" disabled={busy} onClick={() => void deleteWatch()}>{busy ? "삭제 중" : "삭제"}</AlertDialogAction></AlertDialogFooter>
    </AlertDialogContent></AlertDialog>
  </div>;
}
