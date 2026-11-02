"use client";

import React from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { Pause, Radar } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import type { MemberWatchStatus, Watch } from "@/types/tower";
import { watchApi } from "@/lib/api";
import { intervalMinutes } from "@/lib/tower";


export default function EditWatchDialog({ watch, onClose, onSaved }: { readonly watch: Watch; readonly onClose: () => void; readonly onSaved: () => void }) {
  const id = React.useId();
  const [name, setName] = React.useState(watch.name);
  const [minutes, setMinutes] = React.useState(intervalMinutes(watch.fetchInterval));
  const [included, setIncluded] = React.useState(watch.includeInStat);
  const [status, setStatus] = React.useState<MemberWatchStatus>(watch.status === "USER_PAUSED" ? "USER_PAUSED" : "CRAWLING_READY");
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
        <Field><FieldLabel>감시 상태</FieldLabel>
          <ToggleGroup value={[status]} variant="outline" spacing={0} aria-label="감시 상태" className="w-full" onValueChange={values => { const value = values[0]; if (value === "CRAWLING_READY" || value === "USER_PAUSED") setStatus(value); }}>
            <ToggleGroupItem value="CRAWLING_READY" className="flex-1"><Radar data-icon="inline-start" />감시중</ToggleGroupItem>
            <ToggleGroupItem value="USER_PAUSED" className="flex-1"><Pause data-icon="inline-start" />일시정지</ToggleGroupItem>
          </ToggleGroup>
          {watch.status === "CRAWLING_ERROR" && <FieldDescription>확인에 실패한 타워는 상세 화면의 다시 확인 버튼으로 감시를 다시 시작해요.</FieldDescription>}
        </Field>
        <Field orientation="horizontal"><Switch id={`${id}-statistics`} checked={included} onCheckedChange={setIncluded} /><FieldLabel htmlFor={`${id}-statistics`}>인기 URL 통계에 포함</FieldLabel></Field>
      </FieldGroup>
      <DialogFooter><Button variant="outline" onClick={onClose} disabled={busy}>취소</Button><Button type="submit" disabled={!valid || busy}>{busy ? "저장 중" : "저장"}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}
