"use client";

import Link from "next/link";
import React from "react";
import { Activity, ArrowRight, CircleAlert, LogIn, Radar, RefreshCw, UserCheck, UserMinus, UserPlus, UsersRound, UserX, type LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import { Progress } from "@/components/ui/progress";
import WatchStatusDot from "@/components/watch/WatchStatusDot";
import type { TowerStats } from "@/types/admin";
import type { WatchStatus } from "@/types/tower";

/** 상태별 타워 수 */
function towerStatusRows(stats: TowerStats): readonly { readonly status: WatchStatus; readonly count: number }[] {
  const counts: readonly [WatchStatus, number][] = [
    ["CRAWLING_READY", stats.crawlingReadyCount],
    ["CRAWLING", stats.crawlingCount],
    ["CRAWLING_ERROR", stats.crawlingErrorCount],
    ["USER_PAUSED", stats.userPausedCount],
    ["ADMIN_PAUSED", stats.adminPausedCount]
  ];
  return counts.map(([status, count]) => ({ status, count }));
}

function KpiCard({ label, value, hint, icon: Icon, tone = "default" }: {
  readonly label: string;
  readonly value: number | undefined;
  readonly hint: string;
  readonly icon: LucideIcon;
  readonly tone?: "default" | "destructive";
}) {
  return <Card className="bg-linear-to-t from-primary/5 to-card">
    <CardHeader>
      <CardDescription>{label}</CardDescription>
      <CardTitle className={cn("text-3xl font-semibold tracking-tight tabular-nums", tone === "destructive" && value !== undefined && value > 0 && "text-destructive")}>
        {value === undefined ? <Skeleton className="h-9 w-16" /> : value.toLocaleString("ko-KR")}
      </CardTitle>
      <CardAction><Icon className="text-muted-foreground" aria-hidden="true" /></CardAction>
    </CardHeader>
    <CardFooter><p className="text-xs text-muted-foreground">{hint}</p></CardFooter>
  </Card>;
}

function ShareRow({ label, count, total }: { readonly label: React.ReactNode; readonly count: number; readonly total: number }) {
  return <div className="grid grid-cols-[7.5rem_minmax(0,1fr)_3rem] items-center gap-3">
    <div className="min-w-0">{label}</div>
    <Progress value={total === 0 ? 0 : count / total * 100} aria-hidden="true" />
    <p className="text-right text-sm font-medium tabular-nums">{count.toLocaleString("ko-KR")}</p>
  </div>;
}

export default function AdminPage() {
  const towers = useApiData(() => adminApi.getWatchStats().then(r => r.data), []);
  const members = useApiData(() => adminApi.getMemberStats().then(r => r.data), []);
  const auth = useApiData(() => adminApi.getAuthStats().then(r => r.data), []);
  const runningCount = towers.data ? towers.data.crawlingReadyCount + towers.data.crawlingCount : undefined;

  return <main className="flex w-full flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div className="flex flex-col gap-1"><h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">관리자 대시보드</h1>
        <p className="text-sm text-muted-foreground">타워와 회원 현황을 한눈에 확인해요.</p></div>
      <Button variant="outline" onClick={() => { towers.refetch(); members.refetch(); auth.refetch(); }}><RefreshCw data-icon="inline-start" />새로고침</Button>
    </div>

    <section aria-label="핵심 지표" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <KpiCard label="전체 타워" value={towers.data?.totalCount} hint="삭제되지 않은 모든 타워" icon={Radar} />
      <KpiCard label="감시중" value={runningCount} hint="다음 확인을 기다리거나 확인 중인 타워" icon={Activity} />
      <KpiCard label="확인 실패" value={towers.data?.crawlingErrorCount} hint="회원이 다시 확인해야 하는 타워" icon={CircleAlert} tone="destructive" />
      <KpiCard label="전체 회원" value={members.data?.totalCount} hint={members.data ? `활성 회원 ${members.data.activeCount.toLocaleString("ko-KR")}명` : "회원 수를 불러오고 있어요"} icon={UsersRound} />
    </section>

    <div className="grid min-w-0 gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader><CardTitle role="heading" aria-level={2}>타워 상태</CardTitle>
          <CardDescription>등록된 타워가 어떤 상태인지 비율로 보여줘요.</CardDescription>
          <CardAction><Link href="/admin/watches" className={buttonVariants({ variant: "ghost", size: "sm" })}>타워 관리<ArrowRight data-icon="inline-end" /></Link></CardAction></CardHeader>
        <CardContent className="flex flex-col gap-4">
          {towers.loading && towers.data === null && <div role="status" aria-label="타워 통계 불러오는 중"><Skeleton className="h-40 w-full" /></div>}
          {towers.error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{towers.error}</AlertDescription></Alert>}
          {!towers.error && towers.data && <div aria-label="타워 통계" className="flex flex-col gap-4">
            {towerStatusRows(towers.data).map(({ status, count }) => <ShareRow key={status} label={<WatchStatusDot status={status} />} count={count} total={towers.data?.totalCount ?? 0} />)}
          </div>}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle role="heading" aria-level={2}>회원 상태</CardTitle>
          <CardDescription>전체 회원 중 활성, 정지, 탈퇴 회원의 비율이에요.</CardDescription>
          <CardAction><Link href="/admin/members" className={buttonVariants({ variant: "ghost", size: "sm" })}>회원 관리<ArrowRight data-icon="inline-end" /></Link></CardAction></CardHeader>
        <CardContent className="flex flex-col gap-4">
          {members.loading && members.data === null && <div role="status" aria-label="회원 통계 불러오는 중"><Skeleton className="h-40 w-full" /></div>}
          {members.error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{members.error}</AlertDescription></Alert>}
          {!members.error && members.data && <div aria-label="회원 통계" className="flex flex-col gap-4">
            <ShareRow label={<Badge variant="secondary"><UserCheck data-icon="inline-start" />활성</Badge>} count={members.data.activeCount} total={members.data.totalCount} />
            <ShareRow label={<Badge variant="destructive"><UserX data-icon="inline-start" />정지</Badge>} count={members.data.bannedCount} total={members.data.totalCount} />
            <ShareRow label={<Badge variant="outline"><UserMinus data-icon="inline-start" />탈퇴</Badge>} count={members.data.deletedCount} total={members.data.totalCount} />
          </div>}
        </CardContent>
      </Card>
      <Card className="lg:col-span-2">
        <CardHeader><CardTitle role="heading" aria-level={2}>가입 및 로그인</CardTitle>
          <CardDescription>최근 7일과 30일의 이용 현황을 비교해요.</CardDescription></CardHeader>
        <CardContent>
          {auth.loading && auth.data === null && <div role="status" aria-label="인증 통계 불러오는 중"><Skeleton className="h-28 w-full" /></div>}
          {auth.error && <Alert variant="destructive"><CircleAlert /><AlertDescription>{auth.error}</AlertDescription></Alert>}
          {!auth.error && auth.data && <dl aria-label="가입 및 로그인 통계" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              { label: "가입 (최근 7일)", value: auth.data.signUps.last7Days, icon: UserPlus },
              { label: "가입 (최근 30일)", value: auth.data.signUps.last30Days, icon: UserPlus },
              { label: "로그인 (최근 7일)", value: auth.data.signIns.last7Days, icon: LogIn },
              { label: "로그인 (최근 30일)", value: auth.data.signIns.last30Days, icon: LogIn }
            ].map(({ label, value, icon: Icon }) => <div key={label} className="flex items-center gap-3 rounded-lg bg-muted/50 p-4">
              <div className="flex size-9 items-center justify-center rounded-md bg-background ring-1 ring-foreground/10"><Icon className="size-4 text-muted-foreground" aria-hidden="true" /></div>
              <div><dt className="text-xs text-muted-foreground">{label}</dt><dd className="text-xl font-semibold tabular-nums">{value.toLocaleString("ko-KR")}</dd></div>
            </div>)}
          </dl>}
        </CardContent>
      </Card>
    </div>
  </main>;
}
