"use client";

import React from "react";
import Link from "next/link";
import { Bell, ExternalLink, RefreshCw } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia } from "@/components/ui/empty";
import { Field, FieldDescription, FieldGroup, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field";
import { notificationApi } from "@/lib/api/notification";
import { useApiData } from "@/lib/useApiData";

export default function ChannelSelection({ selected, onChange }: {
  readonly selected: readonly number[];
  readonly onChange: (ids: readonly number[]) => void;
}) {
  const id = React.useId();
  const [cursor, setCursor] = React.useState<number | undefined>();
  const [history, setHistory] = React.useState<(number | undefined)[]>([]);
  const { data, loading, error, refetch } = useApiData(() => notificationApi.listChannels({ cursorId: cursor }).then(r => r.data), [cursor]);
  return <FieldSet>
    <FieldLegend>알림 채널</FieldLegend>
    <FieldDescription>타워에 연결할 채널을 선택해 주세요.</FieldDescription>
    {loading && <p role="status" className="text-sm text-muted-foreground">채널 불러오는 중</p>}
    {error && <Alert variant="destructive"><AlertDescription>{error}<Button variant="outline" size="sm" onClick={refetch}>재시도</Button></AlertDescription></Alert>}
    {!loading && !error && data?.data.length === 0 && <Empty className="border"><EmptyHeader><EmptyMedia variant="icon"><Bell /></EmptyMedia><EmptyDescription>등록된 알림 채널이 없어요.</EmptyDescription></EmptyHeader></Empty>}
    <FieldGroup>{!loading && !error && data?.data.map(channel => <Field key={channel.id} orientation="horizontal">
      <Checkbox id={`${id}-${channel.id}`} checked={selected.includes(channel.id)} onCheckedChange={checked => onChange(checked ? [...selected, channel.id] : selected.filter(channelId => channelId !== channel.id))} />
      <FieldLabel htmlFor={`${id}-${channel.id}`}>{channel.name} ({channel.type})</FieldLabel>
    </Field>)}</FieldGroup>
    {(history.length > 0 || data?.hasNext) && <div className="flex flex-wrap gap-2">
      <Button size="sm" variant="outline" disabled={loading || history.length === 0} onClick={() => { setCursor(history[history.length - 1]); setHistory(history.slice(0, -1)); }}>이전 채널</Button>
      <Button size="sm" variant="outline" disabled={loading || !data?.hasNext || data.nextCursorId === null} onClick={() => { if (data?.nextCursorId != null) { setHistory([...history, cursor]); setCursor(data.nextCursorId); } }}>다음 채널</Button>
    </div>}
    <FieldDescription>선택한 채널 {selected.length}개. 채널은 타워를 등록할 때 연결돼요.</FieldDescription>
    <div className="flex flex-wrap gap-2">
      <Link className={buttonVariants({ size: "sm", variant: "ghost" })} href="/mypage/notifications" target="_blank" rel="noopener noreferrer">알림 채널 관리<ExternalLink data-icon="inline-end" /></Link>
      <Button size="sm" variant="ghost" onClick={refetch}><RefreshCw data-icon="inline-start" />채널 새로고침</Button>
    </div>
  </FieldSet>;
}
