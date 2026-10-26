"use client";

import React from "react";
import { CircleAlert, UsersRound } from "lucide-react";
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { TableCell, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { formatDateTime } from "@/lib/dateTime";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import type { AdminMember } from "@/types/admin";
import type { MemberRole } from "@/types/domain";
import { defaultSearch, type SearchCriteria } from "@/types/pagination";
import ListControls from "@/components/common/ListControls";
import ResponsiveTable from "@/components/common/ResponsiveTable";

const roles = [{ value: "NORMAL", label: "일반 회원" }, { value: "ADMIN", label: "관리자" }];

export default function AdminMembersPage() {
  const [criteria, setCriteria] = React.useState<SearchCriteria>(defaultSearch);
  const [page, setPage] = React.useState(0);
  const [target, setTarget] = React.useState<{ readonly member: AdminMember; readonly action: "role" | "ban" } | null>(null);
  const [role, setRole] = React.useState<MemberRole>("NORMAL");
  const [reason, setReason] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);
  const { data, loading, error, refetch } = useApiData(() => adminApi.getMembers({ ...criteria, page }).then(r => r.data), [criteria, page]);

  async function update() {
    if (target === null || busy) return;
    setBusy(true); setActionError(null);
    try {
      if (target.action === "role") await adminApi.updateMemberRole(target.member.id, role);
      else await adminApi.updateMemberStatus(target.member.id, { banned: target.member.bannedAt === null, banReason: reason });
      setTarget(null); refetch();
    } catch (failure) { if (!(failure instanceof Error)) throw failure; setActionError(failure.message); }
    finally { setBusy(false); }
  }

  return <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-5 py-7 sm:px-8 sm:py-10">
    <div className="flex flex-col gap-2"><h1 className="text-[28px] font-semibold tracking-tight sm:text-3xl">회원 관리</h1>
      <p className="text-sm text-muted-foreground">회원 정보와 이용 상태를 확인하고 권한을 관리해요.</p></div>
    <ListControls value={criteria} label="닉네임 또는 이메일 접두사 검색" onChange={next => { setCriteria(next); setPage(0); }} />
    {loading && <div role="status" aria-label="회원 불러오는 중" className="flex flex-col gap-3"><Skeleton className="h-16 w-full" /><Skeleton className="h-48 w-full" /></div>}
    {error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{error}</AlertDescription><AlertAction><Button variant="outline" size="sm" onClick={refetch}>재시도</Button></AlertAction></Alert>}
    {!loading && !error && data && <>
      <p className="text-sm text-muted-foreground">전체 <strong className="font-semibold text-foreground">{data.totalElements}</strong>명</p>
      {data.data.length > 0 ? <ResponsiveTable headings={["회원", "권한", "상태", "최근 로그인", "관리"]}>
        {data.data.map(member => <TableRow key={member.id}>
          <TableCell data-label="회원"><div className="flex items-center gap-3">
            <Avatar><AvatarImage src={member.profileImageUrl || undefined} alt="" /><AvatarFallback>{member.nickname[0]}</AvatarFallback></Avatar>
            <div className="flex min-w-0 flex-col gap-1"><p className="font-medium">{member.nickname}</p><p className="text-xs text-muted-foreground">{member.email}</p></div>
          </div></TableCell>
          <TableCell data-label="권한"><Badge variant="outline">{member.role === "ADMIN" ? "관리자" : "일반 회원"}</Badge></TableCell>
          <TableCell data-label="상태"><div className="flex flex-col gap-2">
            <Badge variant={member.deletedAt ? "outline" : member.bannedAt ? "destructive" : "secondary"}>{member.deletedAt ? "탈퇴" : member.bannedAt ? "정지" : "정상"}</Badge>
            {member.deletedAt && <p className="text-xs text-muted-foreground">탈퇴: {formatDateTime(member.deletedAt)}</p>}
            {member.bannedAt && <p className="text-xs text-muted-foreground">정지일: {formatDateTime(member.bannedAt)}</p>}
            {member.banReason && <p className="text-xs text-muted-foreground">정지 사유: {member.banReason}</p>}
          </div></TableCell>
          <TableCell data-label="최근 로그인"><p className="text-xs text-muted-foreground">{formatDateTime(member.lastSignInAt)}</p></TableCell>
          <TableCell data-label="관리"><div className="flex flex-wrap gap-1.5">
            <Button variant="outline" size="sm" disabled={busy || member.deletedAt !== null} onClick={() => { setRole(member.role); setActionError(null); setTarget({ member, action: "role" }); }}>권한 변경</Button>
            <Button variant={member.bannedAt ? "outline" : "destructive"} size="sm" disabled={busy || member.deletedAt !== null}
              onClick={() => { setReason(""); setActionError(null); setTarget({ member, action: "ban" }); }}>{member.bannedAt ? "정지 해제" : "정지"}</Button>
          </div></TableCell>
        </TableRow>)}
      </ResponsiveTable> : <Empty className="rounded-xl border border-dashed"><EmptyHeader><EmptyMedia variant="icon"><UsersRound /></EmptyMedia><EmptyTitle>조회된 회원이 없어요.</EmptyTitle><EmptyDescription>다른 이름이나 이메일로 검색해 보세요.</EmptyDescription></EmptyHeader></Empty>}
      <div className="flex items-center justify-end gap-3">
        <Button variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>이전</Button>
        <p className="text-sm tabular-nums text-muted-foreground">{data.totalPages === 0 ? 0 : page + 1} / {data.totalPages}</p>
        <Button variant="outline" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>다음</Button>
      </div>
    </>}
    <Dialog open={target !== null} onOpenChange={open => { if (!open && !busy) setTarget(null); }}>
      <DialogContent showCloseButton={!busy}>
        <form className="flex flex-col gap-6" onSubmit={event => { event.preventDefault(); if (target?.action === "role" || target?.member.bannedAt) void update(); }}>
          <DialogHeader><DialogTitle>{target?.action === "role" ? "권한 변경" : target?.member.bannedAt ? "정지 해제" : "회원 정지"}</DialogTitle>
            <DialogDescription>{target?.member.nickname} 회원의 {target?.action === "role" ? "접근 권한을 변경해요." : target?.member.bannedAt ? "서비스 이용을 다시 허용해요." : "서비스 이용을 제한해요. 정지 사유를 입력해 주세요."}</DialogDescription></DialogHeader>
          {actionError && <Alert variant="destructive"><CircleAlert /><AlertDescription>{actionError}</AlertDescription></Alert>}
          <FieldGroup>
            {target?.action === "role" ? <Field data-disabled={busy}><FieldLabel htmlFor="member-role">권한</FieldLabel>
              <Select items={roles} value={role} disabled={busy} onValueChange={value => { if (value === "NORMAL" || value === "ADMIN") setRole(value); }}>
                <SelectTrigger id="member-role" className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent><SelectGroup>{roles.map(item => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectGroup></SelectContent>
              </Select>
            </Field> : !target?.member.bannedAt && <Field data-disabled={busy}><FieldLabel htmlFor="member-ban-reason">정지 사유</FieldLabel>
              <Textarea id="member-ban-reason" required disabled={busy} rows={3} value={reason} onChange={event => setReason(event.target.value)} /></Field>}
          </FieldGroup>
          <DialogFooter><Button variant="outline" disabled={busy} onClick={() => setTarget(null)}>취소</Button>
            <Button type={target?.action === "ban" && !target.member.bannedAt ? "button" : "submit"}
              onClick={target?.action === "ban" && !target.member.bannedAt ? () => void update() : undefined}
              variant={target?.action === "ban" && !target.member.bannedAt ? "destructive" : "default"}
              disabled={busy || (target?.action === "ban" && !target.member.bannedAt && !reason.trim())}>{busy ? "변경 중" : "확인"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </main>;
}
