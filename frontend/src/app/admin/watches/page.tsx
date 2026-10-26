"use client";

import React from "react";
import { CircleAlert, Radar } from "lucide-react";
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { TableCell, TableRow } from "@/components/ui/table";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import { formatFetchInterval } from "@/lib/tower";
import type { Watch, WatchStatus } from "@/types/tower";
import { defaultSearch, type SearchCriteria } from "@/types/pagination";
import ListControls from "@/components/common/ListControls";
import ResponsiveTable from "@/components/common/ResponsiveTable";
import WatchStatusDot from "@/components/watch/WatchStatusDot";

const statuses = [{ value: "RUNNING", label: "감시중" }, { value: "PAUSED", label: "일시정지" }, { value: "ILLEGAL_SUSPENDED", label: "정책 위반 정지" }];

export default function AdminWatchesPage() {
  const [criteria, setCriteria] = React.useState<SearchCriteria>(defaultSearch);
  const [page, setPage] = React.useState(0);
  const [target, setTarget] = React.useState<Watch | null>(null);
  const [status, setStatus] = React.useState<WatchStatus>("RUNNING");
  const [busy, setBusy] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);
  const { data, loading, error, refetch } = useApiData(() => adminApi.getWatches({ ...criteria, page }).then(r => r.data), [criteria, page]);

  async function update() {
    if (target === null || busy) return;
    setBusy(true); setActionError(null);
    try { await adminApi.updateWatchStatus(target.id, status); setTarget(null); refetch(); }
    catch (failure) { if (!(failure instanceof Error)) throw failure; setActionError(failure.message); }
    finally { setBusy(false); }
  }

  return <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-5 py-7 sm:px-8 sm:py-10">
    <div className="flex flex-col gap-2"><h1 className="text-[28px] font-semibold tracking-tight sm:text-3xl">타워 관리</h1>
      <p className="text-sm text-muted-foreground">등록된 타워를 확인하고 감시 상태를 관리해요.</p></div>
    <ListControls value={criteria} label="타워 이름 접두사 검색" onChange={next => { setCriteria(next); setPage(0); }} />
    {loading && <div role="status" aria-label="타워 불러오는 중" className="flex flex-col gap-3"><Skeleton className="h-16 w-full" /><Skeleton className="h-48 w-full" /></div>}
    {error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{error}</AlertDescription><AlertAction><Button variant="outline" size="sm" onClick={refetch}>재시도</Button></AlertAction></Alert>}
    {!loading && !error && data && <>
      <p className="text-sm text-muted-foreground">전체 <strong className="font-semibold text-foreground">{data.totalElements}</strong>개</p>
      {data.data.length > 0 ? <ResponsiveTable headings={["이름", "URL", "상태", "확인 주기", "관리"]}>
        {data.data.map(watch => <TableRow key={watch.id}>
          <TableCell data-label="이름"><p className="font-medium">{watch.name}</p></TableCell>
          <TableCell data-label="URL"><p className="text-xs text-muted-foreground">{watch.url}</p></TableCell>
          <TableCell data-label="상태"><WatchStatusDot status={watch.status} /></TableCell>
          <TableCell data-label="확인 주기">{formatFetchInterval(watch.fetchInterval)}</TableCell>
          <TableCell data-label="관리"><Button variant="outline" size="sm" onClick={() => { setStatus(watch.status); setActionError(null); setTarget(watch); }}>상태 변경</Button></TableCell>
        </TableRow>)}
      </ResponsiveTable> : <Empty className="rounded-xl border border-dashed"><EmptyHeader><EmptyMedia variant="icon"><Radar /></EmptyMedia><EmptyTitle>조회된 타워가 없어요.</EmptyTitle><EmptyDescription>다른 이름으로 검색해 보세요.</EmptyDescription></EmptyHeader></Empty>}
      <div className="flex items-center justify-end gap-3">
        <Button variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>이전</Button>
        <p className="text-sm tabular-nums text-muted-foreground">{data.totalPages === 0 ? 0 : page + 1} / {data.totalPages}</p>
        <Button variant="outline" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>다음</Button>
      </div>
    </>}
    <Dialog open={target !== null} onOpenChange={open => { if (!open && !busy) setTarget(null); }}>
      <DialogContent showCloseButton={!busy}>
        <DialogHeader><DialogTitle>타워 상태 변경</DialogTitle><DialogDescription>{target?.name} 타워의 감시 상태를 변경해요.</DialogDescription></DialogHeader>
        {actionError && <Alert variant="destructive"><CircleAlert /><AlertDescription>{actionError}</AlertDescription></Alert>}
        <FieldGroup><Field data-disabled={busy}><FieldLabel htmlFor="tower-status">상태</FieldLabel>
          <Select items={statuses} value={status} disabled={busy} onValueChange={value => { if (value === "RUNNING" || value === "PAUSED" || value === "ILLEGAL_SUSPENDED") setStatus(value); }}>
            <SelectTrigger id="tower-status" className="w-full"><SelectValue /></SelectTrigger>
            <SelectContent><SelectGroup>{statuses.map(item => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectGroup></SelectContent>
          </Select>
        </Field></FieldGroup>
        <DialogFooter><Button variant="outline" disabled={busy} onClick={() => setTarget(null)}>취소</Button>
          <Button variant={status === "ILLEGAL_SUSPENDED" ? "destructive" : "default"} disabled={busy} onClick={() => void update()}>{busy ? "변경 중" : "변경"}</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  </main>;
}
