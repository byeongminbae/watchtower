"use client";

import React from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { WatchConditionInput } from "@/types/tower";
import { isConditionComplete } from "@/lib/tower";
import ConditionFields from "./ConditionFields";

export default function AddConditionDialog({ open, onClose, onAdd, initial }: {
  readonly open: boolean;
  readonly onClose: () => void;
  readonly onAdd: (condition: WatchConditionInput) => Promise<void>;
  readonly initial?: WatchConditionInput;
}) {
  const [condition, setCondition] = React.useState<WatchConditionInput>(initial ?? { type: "HTML" });
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (busy || !isConditionComplete(condition)) return;
    setBusy(true); setError(null);
    try { await onAdd(condition); onClose(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }
  return <Dialog open={open} onOpenChange={nextOpen => { if (!nextOpen && !busy) onClose(); }}><DialogContent showCloseButton={!busy}>
    <form onSubmit={submit} className="flex min-w-0 flex-col gap-6">
      <DialogHeader><DialogTitle>{initial ? "감지 조건 수정" : "감지 조건 추가"}</DialogTitle><DialogDescription>알림을 받고 싶은 변화만 골라 설정해요.</DialogDescription></DialogHeader>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      <ConditionFields value={condition} onChange={setCondition} lockType={initial !== undefined} />
      <DialogFooter><Button variant="outline" onClick={onClose} disabled={busy}>취소</Button><Button type="submit" disabled={busy || !isConditionComplete(condition)}>{busy ? "저장 중" : "저장"}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}
