"use client";

import React from "react";
import Link from "next/link";
import { CheckCircle2, CircleAlert, UserRound } from "lucide-react";
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialogAction, AlertDialogCancel, AlertDialog, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDateTime } from "@/lib/dateTime";
import { useAuth } from "@/lib/AuthContext";
import { memberApi } from "@/lib/api";

export default function MyPage() {
  const { member, memberStatus, refreshMember } = useAuth();
  const [editing, setEditing] = React.useState(false);
  const [nickname, setNickname] = React.useState("");
  const [deleteOpen, setDeleteOpen] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [message, setMessage] = React.useState<{ readonly severity: "success" | "error"; readonly text: string } | null>(null);

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (member === null || busy) return;
    setBusy(true); setMessage(null);
    try {
      await memberApi.updateMember(member.id, { nickname, profileImageUrl: member.profileImageUrl });
      await refreshMember(); setEditing(false); setMessage({ severity: "success", text: "프로필을 저장했어요." });
    } catch (reason) { if (!(reason instanceof Error)) throw reason; setMessage({ severity: "error", text: reason.message }); }
    finally { setBusy(false); }
  }

  async function withdraw() {
    if (member === null) return;
    setBusy(true); setMessage(null);
    try { await memberApi.deleteMember(member.id); globalThis.location.replace("/"); }
    catch (reason) { if (!(reason instanceof Error)) throw reason; setMessage({ severity: "error", text: reason.message }); }
    finally { setBusy(false); }
  }

  return <main className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-5 py-7 sm:px-8 sm:py-10">
    <div className="flex flex-col gap-2">
      <h1 className="text-[28px] font-semibold tracking-tight sm:text-3xl">마이페이지</h1>
      <p className="text-sm text-muted-foreground">계정 정보를 확인하고 닉네임을 변경해요.</p>
    </div>
    {message && <Alert variant={message.severity === "error" ? "destructive" : "default"}>
      {message.severity === "error" ? <CircleAlert /> : <CheckCircle2 />}<AlertDescription>{message.text}</AlertDescription>
    </Alert>}
    {member === null && memberStatus !== "error" && <div role="status" aria-label="프로필 불러오는 중" className="flex flex-col gap-4">
      <Skeleton className="h-28 w-full" /><Skeleton className="h-64 w-full" />
    </div>}
    {memberStatus === "error" && <Alert variant="destructive"><CircleAlert />
      <AlertDescription>프로필을 불러올 수 없어요.</AlertDescription>
      <AlertAction><Button variant="outline" size="sm" onClick={() => void refreshMember()}>재시도</Button></AlertAction>
    </Alert>}
    {member && <>
      <Card>
        <CardHeader>
          <CardTitle role="heading" aria-level={2}>내 계정</CardTitle>
          <CardAction><UserRound className="size-5 text-muted-foreground" aria-hidden="true" /></CardAction>
        </CardHeader>
        <CardContent className="flex items-center gap-4">
          <Avatar className="size-16">
            <AvatarImage src={member.profileImageUrl || undefined} alt={`${member.nickname} 프로필 사진`} />
            <AvatarFallback>{member.nickname[0]}</AvatarFallback>
          </Avatar>
          <div className="flex min-w-0 flex-col gap-1">
            <p className="break-all text-lg font-semibold">{member.nickname}</p>
            <p className="break-all text-sm text-muted-foreground">{member.email}</p>
            <p className="text-xs text-muted-foreground">최근 로그인: {formatDateTime(member.lastSignInAt)}</p>
          </div>
        </CardContent>
      </Card>
      <form onSubmit={save}>
        <Card>
          <CardHeader><CardTitle role="heading" aria-level={2}>프로필 정보</CardTitle>
            <CardDescription>서비스에서 사용할 이름을 설정해요.</CardDescription></CardHeader>
          <CardContent>
            <FieldGroup>
              <Field data-disabled={!editing || busy}>
                <FieldLabel htmlFor="profile-nickname">닉네임</FieldLabel>
                <Input id="profile-nickname" value={editing ? nickname : member.nickname} disabled={!editing || busy} onChange={event => setNickname(event.target.value)} />
              </Field>

            </FieldGroup>
          </CardContent>
          <CardFooter className="justify-end gap-2">
            {editing ? <><Button variant="outline" disabled={busy} onClick={() => setEditing(false)}>취소</Button>
              <Button type="submit" disabled={busy}>{busy ? "저장 중" : "저장"}</Button></>
              : <Button onClick={() => { setNickname(member.nickname); setEditing(true); }}>프로필 수정</Button>}
          </CardFooter>
        </Card>
      </form>

      <div className="flex items-center justify-between gap-3">
        <Link href="/watches" className={buttonVariants({ variant: "ghost" })}>내 타워</Link>
        <Button variant="destructive" onClick={() => setDeleteOpen(true)}>회원 탈퇴</Button>
      </div>
    </>}
    <AlertDialog open={deleteOpen} onOpenChange={open => { if (!busy) setDeleteOpen(open); }}>
      <AlertDialogContent>
        <AlertDialogHeader><AlertDialogTitle>회원 탈퇴</AlertDialogTitle>
          <AlertDialogDescription>탈퇴하면 계정과 등록한 타워, 감지 조건, 알림 채널이 삭제돼요. 계속하시겠어요?</AlertDialogDescription></AlertDialogHeader>
        {message?.severity === "error" && <Alert variant="destructive"><CircleAlert /><AlertDescription>{message.text}</AlertDescription></Alert>}
        <AlertDialogFooter><AlertDialogCancel disabled={busy}>취소</AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={busy} onClick={() => void withdraw()}>{busy ? "탈퇴 중" : "탈퇴"}</AlertDialogAction></AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </main>;
}
