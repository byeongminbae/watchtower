"use client";

import Link from "next/link";
import { ArrowRight, CircleAlert, Radar, RefreshCw, UsersRound } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

export default function AdminPage() {
  const towers = useApiData(() => adminApi.getWatchStats().then(r => r.data), []);
  const members = useApiData(() => adminApi.getMemberStats().then(r => r.data), []);
  const auth = useApiData(() => adminApi.getAuthStats().then(r => r.data), []);

  return <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-5 py-7 sm:px-8 sm:py-10">
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div className="flex flex-col gap-2"><h1 className="text-[28px] font-semibold tracking-tight sm:text-3xl">관리자 대시보드</h1>
        <p className="text-sm text-muted-foreground">타워와 회원 현황을 한눈에 확인해요.</p></div>
      <Button onClick={() => { towers.refetch(); members.refetch(); auth.refetch(); }}><RefreshCw data-icon="inline-start" />새로고침</Button>
    </div>
    <div className="grid min-w-0 gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader><CardTitle role="heading" aria-level={2}>타워 통계</CardTitle>
          <CardDescription>등록된 타워의 현재 감시 상태예요.</CardDescription>
          <CardAction><Radar className="size-5 text-primary" aria-hidden="true" /></CardAction></CardHeader>
        <CardContent>
          {towers.loading && <div role="status" aria-label="타워 통계 불러오는 중"><Skeleton className="h-52 w-full" /></div>}
          {towers.error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{towers.error}</AlertDescription></Alert>}
          {!towers.loading && !towers.error && towers.data && <Table aria-label="타워 통계"><TableBody>
            <TableRow><TableCell>전체</TableCell><TableCell className="text-right"><span className="text-3xl font-semibold tracking-tight tabular-nums">{towers.data.totalCount}</span></TableCell></TableRow>
            <TableRow><TableCell>감시중</TableCell><TableCell className="text-right">{towers.data.runningCount}</TableCell></TableRow>
            <TableRow><TableCell>일시정지</TableCell><TableCell className="text-right">{towers.data.pausedCount}</TableCell></TableRow>
            <TableRow><TableCell>정책 위반 정지</TableCell><TableCell className="text-right">{towers.data.illegalSuspendedCount}</TableCell></TableRow>
          </TableBody></Table>}
        </CardContent>
        <CardFooter className="justify-end"><Link href="/admin/watches" className={buttonVariants({ variant: "ghost", size: "sm" })}>타워 관리<ArrowRight data-icon="inline-end" /></Link></CardFooter>
      </Card>
      <Card>
        <CardHeader><CardTitle role="heading" aria-level={2}>회원 통계</CardTitle>
          <CardDescription>전체 회원과 계정 이용 상태예요.</CardDescription>
          <CardAction><UsersRound className="size-5 text-primary" aria-hidden="true" /></CardAction></CardHeader>
        <CardContent>
          {members.loading && <div role="status" aria-label="회원 통계 불러오는 중"><Skeleton className="h-52 w-full" /></div>}
          {members.error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{members.error}</AlertDescription></Alert>}
          {!members.loading && !members.error && members.data && <Table aria-label="회원 통계"><TableBody>
            <TableRow><TableCell>전체</TableCell><TableCell className="text-right"><span className="text-3xl font-semibold tracking-tight tabular-nums">{members.data.totalCount}</span></TableCell></TableRow>
            <TableRow><TableCell>활성</TableCell><TableCell className="text-right">{members.data.activeCount}</TableCell></TableRow>
            <TableRow><TableCell>정지</TableCell><TableCell className="text-right">{members.data.bannedCount}</TableCell></TableRow>
            <TableRow><TableCell>탈퇴</TableCell><TableCell className="text-right">{members.data.deletedCount}</TableCell></TableRow>
          </TableBody></Table>}
        </CardContent>
        <CardFooter className="justify-end"><Link href="/admin/members" className={buttonVariants({ variant: "ghost", size: "sm" })}>회원 관리<ArrowRight data-icon="inline-end" /></Link></CardFooter>
      </Card>
      <Card className="lg:col-span-2 [--card-spacing:--spacing(6)]">
        <CardHeader><CardTitle role="heading" aria-level={2}>가입 및 로그인 통계</CardTitle>
          <CardDescription>최근 7일과 30일의 이용 현황을 비교해요.</CardDescription></CardHeader>
        <CardContent>
          {auth.loading && <div role="status" aria-label="인증 통계 불러오는 중"><Skeleton className="h-36 w-full" /></div>}
          {auth.error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{auth.error}</AlertDescription></Alert>}
          {!auth.loading && !auth.error && auth.data && <Table aria-label="가입 및 로그인 통계">
            <TableHeader><TableRow><TableHead>구분</TableHead><TableHead className="text-right">최근 7일</TableHead><TableHead className="text-right">최근 30일</TableHead></TableRow></TableHeader>
            <TableBody><TableRow><TableCell>가입</TableCell><TableCell className="text-right">{auth.data.signUps.last7Days}</TableCell><TableCell className="text-right">{auth.data.signUps.last30Days}</TableCell></TableRow>
              <TableRow><TableCell>로그인</TableCell><TableCell className="text-right">{auth.data.signIns.last7Days}</TableCell><TableCell className="text-right">{auth.data.signIns.last30Days}</TableCell></TableRow></TableBody>
          </Table>}
        </CardContent>
      </Card>
    </div>
  </main>;
}
