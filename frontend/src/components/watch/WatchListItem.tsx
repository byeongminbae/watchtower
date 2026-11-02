"use client";

import { Globe } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Item, ItemActions, ItemContent, ItemDescription, ItemMedia, ItemTitle } from "@/components/ui/item";
import { cn } from "@/lib/utils";
import type { Watch } from "@/types/domain";
import WatchStatusDot from "./WatchStatusDot";

export default function WatchListItem({ watch, selected, onClick }: {
  readonly watch: Watch;
  readonly selected: boolean;
  readonly onClick: () => void;
}) {
  return <Item render={<button type="button" aria-pressed={selected} onClick={onClick} />} variant={selected ? "outline" : "default"}
    className={cn("cursor-pointer items-start text-left hover:bg-muted/60", selected && "bg-background shadow-sm hover:bg-background")}>
    <ItemMedia>
      <Avatar size="lg"><AvatarImage src={watch.faviconUrl || undefined} alt="" /><AvatarFallback><Globe aria-hidden="true" /></AvatarFallback></Avatar>
    </ItemMedia>
    <ItemContent className="min-w-0">
      <ItemTitle className="w-full"><span className="min-w-0 flex-1 truncate">{watch.name}</span></ItemTitle>
      <ItemDescription className="truncate">{watch.url}</ItemDescription>
      {watch.latestAiSummary && <ItemDescription className="line-clamp-2">{watch.latestAiSummary}</ItemDescription>}
    </ItemContent>
    <ItemActions>{watch.status === "CRAWLING_READY" ? <WatchStatusDot status={watch.status} showLabel={false} /> : <WatchStatusDot status={watch.status} />}</ItemActions>
  </Item>;
}
