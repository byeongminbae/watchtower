"use client";

import React from "react";
import { ArrowLeft, ArrowRight, Plus, Radar } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Skeleton } from "@/components/ui/skeleton";
import WatchListItem from "@/components/watch/WatchListItem";
import WatchDetailPanel from "@/components/watch/WatchDetailPanel";
import CreateWatchDialog from "@/components/watch/CreateWatchDialog";
import { defaultSearch, type Cursor, type SearchCriteria } from "@/types/pagination";
import ListControls from "@/components/common/ListControls";
import { watchApi } from "@/lib/api";
import { useAuth } from "@/lib/AuthContext";
import { useApiData } from "@/lib/useApiData";

export default function WatchesPage() {
  const { principal } = useAuth();
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [dialogOpen, setDialogOpen] = React.useState(false);
  const [criteria, setCriteria] = React.useState<SearchCriteria>(defaultSearch);
  const [cursor, setCursor] = React.useState<Cursor | undefined>();
  const [history, setHistory] = React.useState<(Cursor | undefined)[]>([]);
  const { data, loading, error, refetch } = useApiData(() => principal === null ? Promise.resolve(null) : watchApi.listMemberWatches(principal.memberId, { ...criteria, cursorId: cursor?.id, cursorValue: cursor?.value ?? undefined }).then(r => r.data), [principal?.memberId, cursor, criteria]);
  const selected = data?.data.find(watch => watch.id === selectedId) ?? data?.data[0];

  function previousPage() {
    setCursor(history[history.length - 1]);
    setHistory(history.slice(0, -1));
    setSelectedId(null);
  }

  return <main className="mx-auto flex w-full max-w-7xl flex-col gap-7 px-4 py-7 sm:px-8 lg:py-10">
    <div className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
      <div><h1 className="text-3xl font-semibold tracking-tight">내 타워</h1><p className="mt-2 text-sm text-muted-foreground">기다리는 페이지의 변화를 한곳에서 확인해요.</p></div>
      <Button onClick={() => setDialogOpen(true)}><Plus data-icon="inline-start" />타워 등록</Button>
    </div>
    <div className="grid min-w-0 items-start gap-6 xl:grid-cols-[minmax(280px,0.8fr)_minmax(0,1.8fr)]">
      <Card>
        <CardHeader className="gap-4">
          <div className="flex items-center justify-between gap-2"><CardTitle>타워 목록</CardTitle>{!loading && !error && data && <Badge variant="outline">{data.data.length}개</Badge>}</div>
          <ListControls value={criteria} label="타워 이름 접두사 검색" onChange={next => { setCriteria(next); setCursor(undefined); setHistory([]); setSelectedId(null); }} />
        </CardHeader>
        <CardContent className="flex min-w-0 flex-col gap-1">
          {loading && <div role="status" className="flex flex-col gap-4"><p className="text-sm text-muted-foreground">타워 불러오는 중</p><Skeleton className="h-24 w-full" /><Skeleton className="h-24 w-full" /></div>}
          {error && <Alert variant="destructive"><AlertDescription>{error}<Button variant="outline" size="sm" onClick={refetch}>재시도</Button></AlertDescription></Alert>}
          {!loading && !error && data && (data.data.length === 0 ? <Empty className="min-h-48 px-2"><EmptyHeader><EmptyMedia variant="icon"><Radar /></EmptyMedia><EmptyTitle>{criteria.query ? "검색 결과가 없어요" : "첫 번째 타워를 만들어보세요"}</EmptyTitle><EmptyDescription>{criteria.query ? "검색 결과가 없어요." : "등록된 타워가 없어요. 타워를 등록해 주세요."}</EmptyDescription></EmptyHeader></Empty> : data.data.map(watch => <WatchListItem key={watch.id} watch={watch} selected={selected?.id === watch.id} onClick={() => setSelectedId(watch.id)} />))}
        </CardContent>
        <CardFooter>
          <nav aria-label="타워 목록 페이지" className="flex w-full justify-between gap-2">
            <Button variant="ghost" size="icon" aria-label="이전" disabled={loading || history.length === 0} onClick={previousPage}><ArrowLeft /></Button>
            <Button variant="ghost" size="icon" aria-label="다음" disabled={loading || Boolean(error) || !data?.hasNext || data.nextCursorId === null} onClick={() => {
              if (data?.nextCursorId != null) { setHistory([...history, cursor]); setCursor({ id: data.nextCursorId, value: data.nextCursorValue }); setSelectedId(null); }
            }}><ArrowRight /></Button>
          </nav>
        </CardFooter>
      </Card>
      <div className="min-w-0">{!loading && !error && selected && <WatchDetailPanel key={selected.id} watch={selected} headingLevel={2} onChanged={refetch} onDeleted={() => { setSelectedId(null); refetch(); }} />}</div>
    </div>
    {dialogOpen && <CreateWatchDialog open onClose={() => setDialogOpen(false)} onSubmit={async payload => { await watchApi.createWatch(payload); setCursor(undefined); setHistory([]); refetch(); }} />}
  </main>;
}
