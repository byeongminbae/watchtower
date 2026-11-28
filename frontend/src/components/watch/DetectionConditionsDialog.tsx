"use client";

import React from "react";
import { Plus, ScanLine, TextSearch } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia } from "@/components/ui/empty";
import { Item, ItemActions, ItemContent, ItemDescription, ItemMedia, ItemTitle } from "@/components/ui/item";
import { Skeleton } from "@/components/ui/skeleton";
import { watchApi } from "@/lib/api";
import type { UseApiDataResult } from "@/lib/useApiData";
import type { WatchCondition, WatchConditionInput } from "@/types/tower";
import AddConditionDialog from "./AddConditionDialog";

export default function DetectionConditionsDialog({ watchId, conditions, onClose, onConditionsChanged }: {
  readonly watchId: number;
  readonly conditions: UseApiDataResult<WatchCondition[]>;
  readonly onClose: () => void;
  readonly onConditionsChanged: () => void;
}) {
  const [editor, setEditor] = React.useState<{ readonly initial?: WatchCondition } | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  async function saveCondition(condition: WatchConditionInput) {
    const initial = editor?.initial;
    if (initial === undefined) await watchApi.createWatchCondition(watchId, condition);
    else await watchApi.updateWatchCondition(watchId, initial.id, condition);
    onConditionsChanged();
  }
  async function removeCondition(id: number) {
    setBusy(true); setError(null);
    try { await watchApi.deleteWatchCondition(watchId, id); onConditionsChanged(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }

  return <Dialog open onOpenChange={open => { if (!open && !busy) onClose(); }}>
    <DialogContent className="sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>감지 조건</DialogTitle>
        <DialogDescription>타워의 감지 방식과 함께, 이 조건에 맞는 변화가 생겨도 알림을 보내요.</DialogDescription>
      </DialogHeader>
      <div className="flex min-w-0 flex-col gap-2">
        {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
        {conditions.loading && conditions.data === null && <div role="status" className="flex flex-col gap-2"><p className="sr-only">조건 불러오는 중</p><Skeleton className="h-14 w-full" /></div>}
        {conditions.error && <Alert variant="destructive"><AlertDescription>{conditions.error}<Button variant="outline" size="sm" onClick={conditions.refetch}>재시도</Button></AlertDescription></Alert>}
        {!conditions.error && conditions.data?.length === 0 && <Empty className="border border-dashed"><EmptyHeader><EmptyMedia variant="icon"><ScanLine /></EmptyMedia><EmptyDescription>등록된 조건이 없어요. 특정 키워드가 나타나거나 사라질 때 알림을 받으려면 조건을 추가해 주세요.</EmptyDescription></EmptyHeader></Empty>}
        {!conditions.error && conditions.data?.map(condition => <Item key={condition.id} variant="outline">
          <ItemMedia variant="icon"><TextSearch /></ItemMedia>
          <ItemContent className="min-w-0">
            <ItemTitle className="wrap-anywhere">{`키워드: ${condition.keyword}`}</ItemTitle>
            <ItemDescription>{condition.satisfiedWhen === "APPEAR" ? "등장할 때 알림" : "사라질 때 알림"}</ItemDescription>
          </ItemContent>
          <ItemActions>
            <Button variant="ghost" size="sm" disabled={busy} onClick={() => setEditor({ initial: condition })}>조건 수정</Button>
            <Button variant="ghost" size="sm" disabled={busy} className="text-destructive hover:text-destructive" onClick={() => void removeCondition(condition.id)}>조건 삭제</Button>
          </ItemActions>
        </Item>)}
      </div>
      <DialogFooter>
        <Button disabled={busy} onClick={() => setEditor({})}><Plus data-icon="inline-start" />조건 추가</Button>
      </DialogFooter>
      {editor !== null && <AddConditionDialog open onClose={() => setEditor(null)} onAdd={saveCondition} initial={editor.initial} />}
    </DialogContent>
  </Dialog>;
}
