"use client";

import React from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import type { Watch, WatchDetection } from "@/types/tower";
import { watchApi } from "@/lib/api";
import { detectsChange, intervalMinutes } from "@/lib/tower";
import DetectionFields from "./DetectionFields";

/** @param conditionCount 등록된 감지 조건 개수. 아직 불러오지 못했으면 null 이고, 이때는 감지 방식 검사를 백엔드에 맡김 */
export default function EditWatchDialog({ watch, conditionCount, onClose, onSaved }: {
  readonly watch: Watch;
  readonly conditionCount: number | null;
  readonly onClose: () => void;
  readonly onSaved: () => void;
}) {
  const id = React.useId();
  const [name, setName] = React.useState(watch.name);
  const [minutes, setMinutes] = React.useState(intervalMinutes(watch.fetchInterval));
  const [included, setIncluded] = React.useState(watch.includeInStat);
  const [detection, setDetection] = React.useState<WatchDetection>({
    detectTextChange: watch.detectTextChange,
    detectImageChange: watch.detectImageChange,
    ignoreDateTime: watch.ignoreDateTime,
    detectionAreaSelector: watch.detectionAreaSelector
  });
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const detecting = conditionCount === null || detectsChange(detection, conditionCount);
  const valid = name.trim().length > 0 && name.length <= 60 && minutes >= 5 && minutes <= 1440 && minutes % 5 === 0 && detecting;
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!valid || busy) return;
    setBusy(true); setError(null);
    try { await watchApi.updateWatch(watch.id, { name, fetchInterval: `PT${minutes}M`, includeInStat: included, ...detection }); onSaved(); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }
  return <Dialog open onOpenChange={nextOpen => { if (!nextOpen && !busy) onClose(); }}><DialogContent className="sm:max-w-lg" showCloseButton={!busy}>
    <form onSubmit={submit} className="flex min-w-0 flex-col gap-6">
      <DialogHeader><DialogTitle>타워 수정</DialogTitle><DialogDescription>타워 이름과 확인 주기, 감지 방식을 변경할 수 있어요.</DialogDescription></DialogHeader>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      <FieldGroup>
        <Field><FieldLabel htmlFor={`${id}-name`}>사이트 이름</FieldLabel><Input id={`${id}-name`} required value={name} maxLength={60} onChange={event => setName(event.target.value)} /><FieldDescription>수정할 이름은 60자까지 입력할 수 있어요.</FieldDescription></Field>
        <Field><FieldLabel htmlFor={`${id}-interval`}>확인 주기 (분)</FieldLabel><Input id={`${id}-interval`} type="number" required min={5} max={1440} step={5} value={minutes} onChange={event => setMinutes(Number(event.target.value))} /></Field>
        <Field orientation="horizontal"><Switch id={`${id}-statistics`} checked={included} onCheckedChange={setIncluded} /><FieldLabel htmlFor={`${id}-statistics`}>인기 URL 통계에 포함</FieldLabel></Field>
        <DetectionFields value={detection} onChange={setDetection} detecting={detecting} />
      </FieldGroup>
      <DialogFooter><Button variant="outline" onClick={onClose} disabled={busy}>취소</Button><Button type="submit" disabled={!valid || busy}>{busy ? "저장 중" : "저장"}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}
