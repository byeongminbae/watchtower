"use client";

import { Globe } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import type { Watch } from "@/types/domain";
import WatchStatusDot from "./WatchStatusDot";

export default function WatchListItem({ watch, selected, onClick }: {
  readonly watch: Watch;
  readonly selected: boolean;
  readonly onClick: () => void;
}) {
  return <Button variant={selected ? "secondary" : "ghost"} aria-pressed={selected} onClick={onClick}
    className="h-auto w-full items-start justify-start gap-3 px-3 py-4 whitespace-normal">
    <Avatar size="lg"><AvatarImage src={watch.faviconUrl || undefined} alt="" /><AvatarFallback><Globe aria-hidden="true" /></AvatarFallback></Avatar>
    <span className="flex min-w-0 flex-1 flex-col gap-1 text-left">
      <span className="flex items-center gap-2"><WatchStatusDot status={watch.status} showLabel={false} /><span className="min-w-0 wrap-anywhere">{watch.name}</span></span>
      <span className="truncate text-xs text-muted-foreground">{watch.url}</span>
      {watch.latestAiSummary && <span className="mt-1 line-clamp-2 text-xs leading-relaxed font-normal text-muted-foreground">{watch.latestAiSummary}</span>}
    </span>
  </Button>;
}
