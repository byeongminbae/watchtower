"use client";

import { useRouter, useParams } from "next/navigation";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import WatchDetailPanel from "@/components/watch/WatchDetailPanel";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

export default function WatchDetailPage() {
  const { watchId: rawId } = useParams<{ watchId: string }>();
  const id = Number(rawId);
  const valid = /^[1-9]\d*$/.test(rawId) && Number.isSafeInteger(id);
  const router = useRouter();
  const { data, loading, error, refetch } = useApiData(() => valid ? watchApi.getWatch(id).then(r => r.data) : Promise.resolve(null), [id, valid]);
  return <main className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
    {!valid && <Alert variant="destructive"><AlertDescription>올바르지 않은 타워 주소예요.</AlertDescription></Alert>}
    {loading && data === null && <div role="status" className="flex flex-col gap-4"><p className="sr-only">타워 불러오는 중</p><Skeleton className="h-32 w-full" /><Skeleton className="h-28 w-full" /><Skeleton className="h-60 w-full" /></div>}
    {error && <Alert variant="destructive"><AlertDescription>{error}<Button variant="outline" size="sm" onClick={refetch}>재시도</Button></AlertDescription></Alert>}
    {!error && data && <WatchDetailPanel watch={data} onChanged={refetch} onDeleted={() => router.replace("/watches")} />}
  </main>;
}
