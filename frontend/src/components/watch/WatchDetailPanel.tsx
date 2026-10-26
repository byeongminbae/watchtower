"use client";

import React from "react";
import { Edit3, Globe, Pause, Play, Plus, ScanLine, ShieldBan, Sparkles, Trash2, X } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialogAction, AlertDialogCancel, AlertDialog, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia } from "@/components/ui/empty";
import { Separator } from "@/components/ui/separator";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Spinner } from "@/components/ui/spinner";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import type { Watch, WatchCondition, WatchConditionInput } from "@/types/tower";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import WatchActivity from "./WatchActivity";
import SnapshotTimeline from "./SnapshotTimeline";
import AddConditionDialog from "./AddConditionDialog";
import EditWatchDialog from "./EditWatchDialog";

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
  const suspended = watch.status === "ILLEGAL_SUSPENDED";
  const Heading = headingLevel === 1 ? "h1" : "h2";

  async function toggleStatus() {
    if (suspended || busy) return;
    setBusy(true); setError(null);
    try {
      await watchApi.updateWatch(watch.id, {
        name: watch.name,
        fetchInterval: watch.fetchInterval,
        includeInStat: watch.includeInStat,
        status: watch.status === "RUNNING" ? "PAUSED" : "RUNNING"
      });
      onChanged?.();
    } catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
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
  return <>
    <Card>
      <CardHeader className="gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <Avatar size="lg"><AvatarImage src={watch.faviconUrl || undefined} alt="" /><AvatarFallback><Globe aria-hidden="true" /></AvatarFallback></Avatar>
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <CardTitle><Heading className="text-xl font-semibold tracking-tight wrap-anywhere">{watch.name}</Heading></CardTitle>
            <CardDescription><a href={watch.url} target="_blank" rel="noopener noreferrer" className="wrap-anywhere underline underline-offset-4">{watch.url}</a></CardDescription>
          </div>
          <Tooltip>
            <TooltipTrigger render={<Button variant={watch.status === "RUNNING" ? "secondary" : "outline"} size="icon" disabled={suspended || busy} onClick={() => void toggleStatus()} />} aria-label={suspended ? "정책 위반으로 감시 정지" : watch.status === "RUNNING" ? "감시 일시정지" : "감시 시작"}>
              {busy ? <Spinner aria-label="변경 중" /> : suspended ? <ShieldBan /> : watch.status === "RUNNING" ? <Pause /> : <Play />}
            </TooltipTrigger>
            <TooltipContent>{suspended ? "정책 위반으로 정지된 타워예요." : watch.status === "RUNNING" ? "감시 중이에요. 눌러서 일시정지해요." : "일시정지 중이에요. 눌러서 다시 시작해요."}</TooltipContent>
          </Tooltip>
        </div>
        <div><Button variant="outline" disabled={suspended || busy} onClick={() => setEditOpen(true)}><Edit3 data-icon="inline-start" />타워 수정</Button></div>
      </CardHeader>
      <CardContent className="flex min-w-0 flex-col gap-6">
        {error && !deleteOpen && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription><Button variant="ghost" size="icon-sm" aria-label="오류 닫기" onClick={() => setError(null)}><X /></Button></Alert>}
        {suspended && <Alert variant="destructive"><AlertDescription>정책 위반으로 감시가 정지되었어요. 타워 설정을 수정할 수 없어요.</AlertDescription></Alert>}
        <WatchActivity watch={watch} />
        {watch.latestAiSummary && <Alert><Sparkles /><AlertTitle>최근 변화 요약</AlertTitle><AlertDescription>{watch.latestAiSummary}</AlertDescription></Alert>}
        <Separator />
        <section className="flex min-w-0 flex-col gap-4">
          <div className="flex items-center justify-between gap-3"><div><h2 className="text-base font-semibold">감지 조건</h2><p className="mt-1 text-sm text-muted-foreground">알림을 받을 변화를 설정해요.</p></div><Button variant="outline" size="sm" disabled={busy} onClick={() => setConditionDialog({})}><Plus data-icon="inline-start" />조건 추가</Button></div>
          {conditions.loading && <p role="status" className="text-sm text-muted-foreground">조건 불러오는 중</p>}
          {conditions.error && <Alert variant="destructive"><AlertDescription>{conditions.error}<Button variant="outline" size="sm" onClick={conditions.refetch}>재시도</Button></AlertDescription></Alert>}
          {!conditions.loading && !conditions.error && conditions.data?.length === 0 && <Empty className="border"><EmptyHeader><EmptyMedia variant="icon"><ScanLine /></EmptyMedia><EmptyDescription>등록된 조건이 없어요. 조건을 추가해 주세요.</EmptyDescription></EmptyHeader></Empty>}
          {!conditions.loading && !conditions.error && conditions.data?.map(condition => <div key={condition.id} className="flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center">
            <div className="min-w-0 flex-1"><p className="text-sm font-medium wrap-anywhere">{condition.type === "HTML" ? "전체 변경 감지" : condition.type === "KEYWORD" ? `키워드: ${condition.keyword}` : `정규식: ${condition.regex}`}</p>{condition.type !== "HTML" && <p className="mt-1 text-xs text-muted-foreground">{condition.isSatisfiedAtAppearance ? "등장할 때 알림" : "사라질 때 알림"}</p>}</div>
            <div className="flex shrink-0 gap-1">{condition.type !== "HTML" && <Button variant="ghost" size="sm" disabled={busy} onClick={() => setConditionDialog({ initial: condition })}>조건 수정</Button>}<Button variant="destructive" size="sm" disabled={busy} onClick={() => void removeCondition(condition.id)}>조건 삭제</Button></div>
          </div>)}
        </section>
        <Separator />
        {snapshots.loading && <p role="status" className="text-sm text-muted-foreground">스냅샷 불러오는 중</p>}
        {snapshots.error && <Alert variant="destructive"><AlertDescription>{snapshots.error}<Button variant="outline" size="sm" onClick={snapshots.refetch}>재시도</Button></AlertDescription></Alert>}
        {!snapshots.loading && !snapshots.error && snapshots.data && <SnapshotTimeline snapshots={snapshots.data} />}
      </CardContent>
      <CardFooter className="justify-end"><Button variant="destructive" size="sm" disabled={busy} onClick={() => setDeleteOpen(true)}><Trash2 data-icon="inline-start" />타워 삭제</Button></CardFooter>
    </Card>
    {conditionDialog !== null && <AddConditionDialog open onClose={() => setConditionDialog(null)} onAdd={saveCondition} initial={conditionDialog.initial} />}
    {editOpen && <EditWatchDialog watch={watch} onClose={() => setEditOpen(false)} onSaved={() => { setEditOpen(false); onChanged?.(); }} />}
    <AlertDialog open={deleteOpen} onOpenChange={open => { if (!busy) setDeleteOpen(open); }}><AlertDialogContent>
      <AlertDialogHeader><AlertDialogTitle>타워를 삭제할까요?</AlertDialogTitle><AlertDialogDescription>이 타워의 감시를 종료하고 목록에서 삭제해요.</AlertDialogDescription></AlertDialogHeader>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      <AlertDialogFooter><AlertDialogCancel disabled={busy}>취소</AlertDialogCancel><AlertDialogAction variant="destructive" disabled={busy} onClick={() => void deleteWatch()}>{busy ? "삭제 중" : "삭제"}</AlertDialogAction></AlertDialogFooter>
    </AlertDialogContent></AlertDialog>
  </>;
}
