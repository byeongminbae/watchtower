"use client";

import * as React from "react";
import { TrendingUp } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

const ROTATION_INTERVAL_MS = 5_000;
const MAX_HOT_URLS = 10;

export default function HotUrlSection() {
  const { data: hotUrls, loading, error } = useApiData(() => watchApi.getTrendingWatches().then(r => r.data), []);
  const rankedUrls = hotUrls?.slice(0, MAX_HOT_URLS) ?? [];
  const [activeIndex, setActiveIndex] = React.useState(0);

  React.useEffect(() => {
    if (rankedUrls.length < 2) return;
    const motionPreference = window.matchMedia("(prefers-reduced-motion: reduce)");
    let intervalId: number | undefined;
    function syncRotation() {
      window.clearInterval(intervalId);
      if (!motionPreference.matches) {
        intervalId = window.setInterval(() => setActiveIndex(current => (current + 1) % rankedUrls.length), ROTATION_INTERVAL_MS);
      }
    }
    syncRotation();
    motionPreference.addEventListener("change", syncRotation);
    return () => { window.clearInterval(intervalId); motionPreference.removeEventListener("change", syncRotation); };
  }, [rankedUrls.length]);

  const displayIndex = rankedUrls.length > 0 ? activeIndex % rankedUrls.length : 0;
  const activeItem = rankedUrls[displayIndex];

  return <section className="border-y" aria-label="최근 변화가 많은 URL">
    <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-4 px-6 py-4 text-sm">
      <p className="flex shrink-0 items-center gap-2 [&>svg]:size-4"><TrendingUp aria-hidden="true" /> 지금 변화가 많은 페이지</p>
      <div className="flex min-w-0 flex-1 items-center gap-3">
        {loading && <Skeleton className="h-5 w-full max-w-80" />}
        {!loading && (error || !activeItem) && <p>인기 감시 URL을 집계하고 있어요.</p>}
        {!loading && activeItem && <>
          <span className="shrink-0 text-xs tabular-nums">{displayIndex + 1}위</span>
          <p className="truncate text-sm text-muted-foreground" title={activeItem.url}>{activeItem.url}</p>
          <Badge variant="secondary">변화 {activeItem.changeCount}회</Badge>
          <span className="ml-auto hidden shrink-0 text-xs tabular-nums text-muted-foreground sm:inline">{displayIndex + 1} / {rankedUrls.length}</span>
        </>}
      </div>
    </div>
  </section>;
}
