"use client";

import React from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import type { CreateWatchRequest } from "@/lib/api/watch";
import type { WatchConditionInput } from "@/types/tower";
import { ApiError } from "@/lib/api/errors";
import { isConditionComplete } from "@/lib/tower";
import ConditionFields from "./ConditionFields";
import ChannelSelection from "./ChannelSelection";

export default function CreateWatchDialog({ open, onClose, onSubmit }: {
  readonly open: boolean;
  readonly onClose: () => void;
  readonly onSubmit: (payload: CreateWatchRequest) => Promise<void>;
}) {
  const id = React.useId();
  const [name, setName] = React.useState("");
  const [url, setUrl] = React.useState("");
  const [condition, setCondition] = React.useState<WatchConditionInput>({ type: "HTML" });
  const [minutes, setMinutes] = React.useState(5);
  const [includeInStat, setIncludeInStat] = React.useState(true);
  const [channels, setChannels] = React.useState<readonly number[]>([]);
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const valid = name.trim().length > 0 && name.length <= 255 && /^https?:\/\//i.test(url) && minutes >= 5 && minutes <= 1440 && minutes % 5 === 0 && isConditionComplete(condition);
  const close = () => { if (!submitting) onClose(); };
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!valid || submitting) return;
    setSubmitting(true); setError(null);
    try {
      await onSubmit({ name, url, fetchInterval: `PT${minutes}M`, includeInStat, notificationChannelIds: channels, conditions: [condition] });
      onClose();
    } catch (reason) {
      if (!(reason instanceof Error)) throw reason;
      setError(reason instanceof ApiError ? reason.message : "타워 등록에 실패했어요.");
    } finally { setSubmitting(false); }
  }
  return <Dialog open={open} onOpenChange={nextOpen => { if (!nextOpen) close(); }}>
    <DialogContent className="sm:max-w-lg" showCloseButton={!submitting}>
      <form onSubmit={submit} className="flex min-w-0 flex-col gap-6">
        <DialogHeader><DialogTitle>새 타워 등록</DialogTitle><DialogDescription>페이지와 감지 조건을 등록하면 변화를 대신 확인해 드려요.</DialogDescription></DialogHeader>
        {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
        <FieldGroup>
          <Field><FieldLabel htmlFor={`${id}-name`}>사이트 이름</FieldLabel><Input id={`${id}-name`} required value={name} onChange={event => setName(event.target.value)} placeholder="예: 채용 공고" maxLength={255} /></Field>
          <Field><FieldLabel htmlFor={`${id}-url`}>감시할 URL</FieldLabel><Input id={`${id}-url`} type="url" required value={url} onChange={event => setUrl(event.target.value)} placeholder="https://example.com" /></Field>
          <Separator />
          <ConditionFields value={condition} onChange={setCondition} />
          <Field><FieldLabel htmlFor={`${id}-interval`}>확인 주기 (분)</FieldLabel><Input id={`${id}-interval`} type="number" required min={5} max={1440} step={5} value={minutes} onChange={event => setMinutes(Number(event.target.value))} /><FieldDescription>5분부터 24시간까지, 5분 단위로 설정해요.</FieldDescription></Field>
          <Field orientation="horizontal"><Switch id={`${id}-statistics`} checked={includeInStat} onCheckedChange={setIncludeInStat} /><FieldLabel htmlFor={`${id}-statistics`}>인기 URL 통계에 포함</FieldLabel></Field>
          <Separator />
          <ChannelSelection selected={channels} onChange={setChannels} />
        </FieldGroup>
        <DialogFooter><Button variant="outline" onClick={close} disabled={submitting}>취소</Button><Button type="submit" disabled={!valid || submitting}>{submitting ? "등록 중" : "등록"}</Button></DialogFooter>
      </form>
    </DialogContent>
  </Dialog>;
}
