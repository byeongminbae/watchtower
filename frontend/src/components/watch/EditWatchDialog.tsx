"use client";

import React from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import type { Watch } from "@/types/tower";
import { watchApi } from "@/lib/api";
import { intervalMinutes } from "@/lib/tower";

const statuses = [{ value: "RUNNING", label: "감시중" }, { value: "PAUSED", label: "일시정지" }];

export default function EditWatchDialog({ watch, onClose, onSaved }: { readonly watch: Watch; readonly onClose: () => void; readonly onSaved: () => void }) {
  const id = React.useId();
  const [name, setName] = React.useState(watch.name);
  const [minutes, setMinutes] = React.useState(intervalMinutes(watch.fetchInterval));
  const [included, setIncluded] = React.useState(watch.includeInStat);
  const [status, setStatus] = React.useState<"RUNNING" | "PAUSED">(watch.status === "PAUSED" ? "PAUSED" : "RUNNING");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const valid = name.trim().length > 0 && name.length <= 60 && minutes >= 5 && minutes <= 1440 && minutes % 5 === 0;
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!valid || busy) return;
    setBusy(true); setError(null);
    try { await watchApi.updateWatch(watch.id, { name, fetchInterval: `PT${minutes}M`, includeInStat: included, status }); onSaved(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }
  return <Dialog open onOpenChange={nextOpen => { if (!nextOpen && !busy) onClose(); }}><DialogContent className="sm:max-w-lg" showCloseButton={!busy}>
    <form onSubmit={submit} className="flex min-w-0 flex-col gap-6">
      <DialogHeader><DialogTitle>타워 수정</DialogTitle><DialogDescription>타워 이름과 확인 주기, 감시 상태를 변경할 수 있어요.</DialogDescription></DialogHeader>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      <FieldGroup>
        <Field><FieldLabel htmlFor={`${id}-name`}>사이트 이름</FieldLabel><Input id={`${id}-name`} required value={name} maxLength={60} onChange={event => setName(event.target.value)} /><FieldDescription>수정할 이름은 60자까지 입력할 수 있어요.</FieldDescription></Field>
        <Field><FieldLabel htmlFor={`${id}-interval`}>확인 주기 (분)</FieldLabel><Input id={`${id}-interval`} type="number" required min={5} max={1440} step={5} value={minutes} onChange={event => setMinutes(Number(event.target.value))} /></Field>
        <Field><FieldLabel htmlFor={`${id}-status`}>상태</FieldLabel>
          <Select items={statuses} value={status} onValueChange={value => { if (value === "RUNNING" || value === "PAUSED") setStatus(value); }}>
            <SelectTrigger id={`${id}-status`} className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectGroup>{statuses.map(item => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectGroup></SelectContent>
          </Select>
        </Field>
        <Field orientation="horizontal"><Switch id={`${id}-statistics`} checked={included} onCheckedChange={setIncluded} /><FieldLabel htmlFor={`${id}-statistics`}>인기 URL 통계에 포함</FieldLabel></Field>
      </FieldGroup>
      <DialogFooter><Button variant="outline" onClick={onClose} disabled={busy}>취소</Button><Button type="submit" disabled={!valid || busy}>{busy ? "저장 중" : "저장"}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}
