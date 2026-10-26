"use client";

import React from "react";
import { CircleAlert } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { notificationApi } from "@/lib/api/notification";
import type { NotificationChannel, NotificationChannelType } from "@/types/notification";

const channelTypes = [{ value: "DISCORD", label: "Discord" }, { value: "TELEGRAM", label: "Telegram" }];

export default function ChannelDialog({ channel, onClose, onSaved }: {
  readonly channel?: NotificationChannel;
  readonly onClose: () => void;
  readonly onSaved: () => void;
}) {
  const [name, setName] = React.useState(channel?.name ?? "");
  const [type, setType] = React.useState<NotificationChannelType>(channel?.type ?? "DISCORD");
  const [url, setUrl] = React.useState(channel?.webhookUrl ?? "");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const valid = name.trim().length > 0 && name.length <= 255 && url.length <= 255 &&
    URL.canParse(url) && new URL(url).protocol === "https:";

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!valid || busy) return;
    setBusy(true); setError(null);
    try {
      if (channel) await notificationApi.updateChannel(channel.id, { name, webhookUrl: url });
      else await notificationApi.createChannel({ name, type, webhookUrl: url });
      onSaved();
    } catch (reason) { if (!(reason instanceof Error)) throw reason; setError(reason.message); }
    finally { setBusy(false); }
  }

  return <Dialog open onOpenChange={open => { if (!open && !busy) onClose(); }}>
    <DialogContent showCloseButton={!busy}>
      <form onSubmit={submit} className="flex flex-col gap-6">
        <DialogHeader><DialogTitle>{channel ? "알림 채널 수정" : "알림 채널 등록"}</DialogTitle>
          <DialogDescription>페이지에 변화가 생기면 이 채널로 알려드려요.</DialogDescription></DialogHeader>
        {error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{error}</AlertDescription></Alert>}
        <FieldGroup>
          <Field data-disabled={busy}><FieldLabel htmlFor="channel-name">채널 이름</FieldLabel>
            <Input id="channel-name" required maxLength={255} disabled={busy} value={name} onChange={event => setName(event.target.value)} placeholder="예: 중요한 소식" /></Field>
          <Field data-disabled={channel !== undefined || busy}><FieldLabel htmlFor="channel-type">채널 종류</FieldLabel>
            <Select items={channelTypes} disabled={channel !== undefined || busy} value={type} onValueChange={value => { if (value === "DISCORD" || value === "TELEGRAM") setType(value); }}>
              <SelectTrigger id="channel-type" className="w-full"><SelectValue /></SelectTrigger>
              <SelectContent><SelectGroup>{channelTypes.map(item => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectGroup></SelectContent>
            </Select>
            {channel && <FieldDescription>등록한 채널의 종류는 변경할 수 없어요.</FieldDescription>}
          </Field>
          <Field data-disabled={busy}><FieldLabel htmlFor="channel-webhook">Webhook URL</FieldLabel>
            <Input id="channel-webhook" required type="url" disabled={busy} value={url} onChange={event => setUrl(event.target.value)} autoComplete="off" spellCheck={false} placeholder="https://" maxLength={255} />
            <FieldDescription>알림을 받을 채널의 HTTPS 주소를 입력해 주세요.</FieldDescription>
          </Field>
        </FieldGroup>
        <DialogFooter><Button variant="outline" disabled={busy} onClick={onClose}>취소</Button>
          <Button type="submit" disabled={!valid || busy}>{busy ? "저장 중" : "저장"}</Button></DialogFooter>
      </form>
    </DialogContent>
  </Dialog>;
}
