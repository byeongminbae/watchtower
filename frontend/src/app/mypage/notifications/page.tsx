"use client";

import React from "react";
import Link from "next/link";
import { ArrowLeft, ArrowRight, Bell, CircleAlert, Plus } from "lucide-react";
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialogAction, AlertDialogCancel, AlertDialog, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Skeleton } from "@/components/ui/skeleton";
import { notificationApi } from "@/lib/api/notification";
import { useApiData } from "@/lib/useApiData";
import type { NotificationChannel } from "@/types/notification";
import { defaultSearch, type Cursor, type SearchCriteria } from "@/types/pagination";
import ListControls from "@/components/common/ListControls";
import ChannelDialog from "@/components/notification/ChannelDialog";

export default function NotificationsPage() {
  const [criteria, setCriteria] = React.useState<SearchCriteria>(defaultSearch);
  const [cursor, setCursor] = React.useState<Cursor | undefined>();
  const [history, setHistory] = React.useState<(Cursor | undefined)[]>([]);
  const [dialog, setDialog] = React.useState<{ readonly channel?: NotificationChannel } | null>(null);
  const [target, setTarget] = React.useState<NotificationChannel | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);
  const { data, loading, error, refetch } = useApiData(() =>
    notificationApi.listChannels({ ...criteria, cursorId: cursor?.id, cursorValue: cursor?.value ?? undefined }).then(r => r.data), [criteria, cursor]);

  async function remove() {
    if (target === null) return;
    setBusy(true); setActionError(null);
    try { await notificationApi.deleteChannel(target.id); setTarget(null); refetch(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setActionError(reason.message); }
    finally { setBusy(false); }
  }

  return <main className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-5 py-7 sm:px-8 sm:py-10">
    <div><Link href="/mypage" className={buttonVariants({ variant: "ghost", size: "sm" })}><ArrowLeft data-icon="inline-start" />마이페이지로</Link></div>
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div className="flex flex-col gap-2"><h1 className="text-[28px] font-semibold tracking-tight sm:text-3xl">알림 채널</h1>
        <p className="text-sm text-muted-foreground">기다리던 소식을 편한 곳에서 받아보세요.</p></div>
      <Button onClick={() => setDialog({})}><Plus data-icon="inline-start" />채널 등록</Button>
    </div>
    <ListControls value={criteria} label="채널 이름 접두사 검색" onChange={next => { setCriteria(next); setCursor(undefined); setHistory([]); }} />
    {actionError && target === null && <Alert variant="destructive"><CircleAlert /><AlertDescription>{actionError}</AlertDescription></Alert>}
    {loading && <div role="status" aria-label="채널 불러오는 중" className="flex flex-col gap-4"><Skeleton className="h-36 w-full" /><Skeleton className="h-36 w-full" /></div>}
    {error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{error}</AlertDescription><AlertAction><Button variant="outline" size="sm" onClick={refetch}>재시도</Button></AlertAction></Alert>}
    {!loading && !error && data?.data.length === 0 && <Empty className="rounded-xl border border-dashed">
      <EmptyHeader><EmptyMedia variant="icon"><Bell /></EmptyMedia><EmptyTitle>등록된 채널이 없어요.</EmptyTitle>
        <EmptyDescription>알림을 받을 채널을 등록해 주세요.</EmptyDescription></EmptyHeader>
      <EmptyContent><Button variant="outline" onClick={() => setDialog({})}><Plus data-icon="inline-start" />첫 채널 등록</Button></EmptyContent>
    </Empty>}
    {!loading && !error && data?.data.map(channel => <Card key={channel.id}>
      <CardHeader className="has-data-[slot=card-action]:grid-cols-[minmax(0,1fr)_auto]">
        <CardTitle role="heading" aria-level={2} className="break-all">{channel.name}</CardTitle>
        <CardAction className="flex flex-wrap justify-end gap-1">
          <Button variant="outline" size="sm" onClick={() => setDialog({ channel })}>수정</Button>
          <Button variant="destructive" size="sm" onClick={() => { setActionError(null); setTarget(channel); }}>삭제</Button>
        </CardAction>
      </CardHeader>
      <CardContent><Badge variant="secondary"><Bell data-icon="inline-start" />{channel.type === "DISCORD" ? "Discord" : "Telegram"}</Badge></CardContent>
    </Card>)}
    <div className="flex justify-end gap-2">
      <Button variant="ghost" size="icon" aria-label="이전" disabled={loading || history.length === 0} onClick={() => { setCursor(history[history.length - 1]); setHistory(history.slice(0, -1)); }}><ArrowLeft /></Button>
      <Button variant="ghost" size="icon" aria-label="다음" disabled={loading || !data?.hasNext || data.nextCursorId === null} onClick={() => {
        if (data?.nextCursorId != null) { setHistory([...history, cursor]); setCursor({ id: data.nextCursorId, value: data.nextCursorValue }); }
      }}><ArrowRight /></Button>
    </div>
    {dialog && <ChannelDialog channel={dialog.channel} onClose={() => setDialog(null)} onSaved={() => { setDialog(null); refetch(); }} />}
    <AlertDialog open={target !== null} onOpenChange={open => { if (!open && !busy) setTarget(null); }}>
      <AlertDialogContent>
        <AlertDialogHeader><AlertDialogTitle>알림 채널 삭제</AlertDialogTitle>
          <AlertDialogDescription>{target?.name} 채널을 삭제할까요? 타워와의 연결도 삭제돼요.</AlertDialogDescription></AlertDialogHeader>
        {actionError && <Alert variant="destructive"><CircleAlert /><AlertDescription>{actionError}</AlertDescription></Alert>}
        <AlertDialogFooter><AlertDialogCancel disabled={busy}>취소</AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={busy} onClick={() => void remove()}>{busy ? "삭제 중" : "삭제"}</AlertDialogAction></AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </main>;
}
