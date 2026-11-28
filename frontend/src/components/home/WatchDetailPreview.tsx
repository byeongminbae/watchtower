"use client";

import {useEffect, useState} from "react";
import {WatchDetailView} from "@/components/watch/WatchDetailPanel";
import type {UseApiDataResult} from "@/lib/useApiData";
import type {Watch, WatchCondition, WatchSnapshot} from "@/types/tower";

const NEW_CHANGE_INTERVAL_MILLIS = 2_000;
const MINUTE_MS = 60_000;
const HOUR_MS = 60 * MINUTE_MS;
const RETENTION_MS = 7 * 24 * HOUR_MS;
const LIVE_SPAN_MS = 4 * HOUR_MS;
const SEED_SLOT_COUNT = 10;
const MAX_CHANGES_PER_SEED_SLOT = 5;
const LIVE_CHANGE_COUNT = 10;
const AI_SUMMARIES = [
    "2027학년도 1학기 수강신청 일정이 새로 올라왔어요.",
    "조기졸업 신청 기간이 12월 5일까지로 늘었어요.",
    "겨울 계절학기 폐강 과목 목록이 추가됐어요.",
    "국가장학금 2차 신청 안내가 새로 올라왔어요.",
    "기말고사 시간표 일부가 바뀌었어요.",
    "졸업논문 제출 마감 공지가 추가됐어요.",
    "교내 근로장학생 모집 공고가 새로 올라왔어요.",
    "휴학 신청 마감일 안내가 수정됐어요."
] as const;
const PREVIEW_WATCH: Watch = {
    id: 0,
    name: "서울대학교 학사 공지",
    url: "https://snu.ac.kr",
    faviconUrl: null,
    status: "READY",
    fetchInterval: "PT5M",
    includeInStat: true,
    detectTextChange: true,
    detectImageChange: false,
    ignoreDateTime: true,
    detectionAreaSelector: null,
    lastFetchedAt: null,
    lastCrawlerResultCode: "FETCHED",
    lastNotifiedAt: null,
    latestAiSummary: null
};
const PREVIEW_CONDITIONS: WatchCondition[] = [{id: 1, type: "KEYWORD", keyword: "장학금", satisfiedWhen: "APPEAR"}];

type Preview = {
    readonly seedSnapshots: WatchSnapshot[];
    readonly snapshots: WatchSnapshot[];
    readonly upcomingDetectedTimes: number[];
};

function randomBetween(min: number, max: number): number {
    return min + (Math.random() * (max - min));
}

function previewSnapshotOf(id: number, detectedTime: number): WatchSnapshot {
    return {
        id,
        detectedAt: new Date(detectedTime).toISOString(),
        hasHighlightedScreenshot: false,
        aiDiffSummary: AI_SUMMARIES[id % AI_SUMMARIES.length],
        notifiedAt: (id % 3) === 0 ? null : new Date(detectedTime).toISOString()
    };
}

function randomTimesBetween(count: number, from: number, to: number): number[] {
    return Array.from({length: count}, () => randomBetween(from, to)).sort((left, right) => left - right);
}

function upcomingDetectedTimesOf(now: number): number[] {
    return randomTimesBetween(LIVE_CHANGE_COUNT, now - LIVE_SPAN_MS, now);
}

function seedSnapshotsOf(now: number): WatchSnapshot[] {
    const seedSlotTimes = randomTimesBetween(SEED_SLOT_COUNT, now - RETENTION_MS + HOUR_MS, now - LIVE_SPAN_MS - HOUR_MS);
    const seedTimes = seedSlotTimes.flatMap(slotTime => {
        const slotStartTime = new Date(slotTime).setMinutes(0, 0, 0);
        return randomTimesBetween(Math.ceil(Math.random() * MAX_CHANGES_PER_SEED_SLOT), slotStartTime, slotStartTime + HOUR_MS);
    });
    return seedTimes.map((time, index) => previewSnapshotOf(index + 1, time));
}

function initialPreviewOf(now: number): Preview {
    return previewRestartedFrom(seedSnapshotsOf(now), now);
}

function previewRestartedFrom(seedSnapshots: WatchSnapshot[], now: number): Preview {
    return nextPreviewOf({seedSnapshots, snapshots: seedSnapshots, upcomingDetectedTimes: upcomingDetectedTimesOf(now)}, now);
}

function nextPreviewOf(preview: Preview, now: number): Preview {
    const [detectedTime, ...upcomingDetectedTimes] = preview.upcomingDetectedTimes;
    if (detectedTime === undefined) {
        return previewRestartedFrom(preview.seedSnapshots, now);
    }
    return {...preview, snapshots: [...preview.snapshots, previewSnapshotOf(preview.snapshots.length + 1, detectedTime)], upcomingDetectedTimes};
}

function watchOf(preview: Preview): Watch {
    const latestSnapshot = preview.snapshots.reduce((latest, snapshot) => snapshot.detectedAt > latest.detectedAt ? snapshot : latest);
    const latestNotifiedAt = preview.snapshots.reduce<string | null>((latest, {notifiedAt}) => (notifiedAt !== null && (latest === null || notifiedAt > latest)) ? notifiedAt : latest, null);
    return {
        ...PREVIEW_WATCH,
        lastFetchedAt: latestSnapshot.detectedAt,
        lastNotifiedAt: latestNotifiedAt,
        latestAiSummary: latestSnapshot.aiDiffSummary
    };
}

function loadedResultOf<T>(data: T): UseApiDataResult<T> {
    return {data, loading: false, error: null, refetch: () => undefined};
}

export default function WatchDetailPreview() {
    const [preview, setPreview] = useState(() => initialPreviewOf(Date.now()));

    useEffect(() => {
        const timer = window.setInterval(() => setPreview(current => nextPreviewOf(current, Date.now())), NEW_CHANGE_INTERVAL_MILLIS);
        return () => window.clearInterval(timer);
    }, []);

    return <div className="flex min-w-0 flex-col gap-2">
        <div inert className="h-[80rem] overflow-hidden rounded-2xl bg-muted/40 p-3 ring-1 ring-foreground/10 mask-b-from-92% md:h-[59rem] [&_ol>li:nth-child(4)]:mask-b-from-0% [&_ol>li:nth-child(n+5)]:hidden">
            <WatchDetailView watch={watchOf(preview)} conditions={loadedResultOf(PREVIEW_CONDITIONS)}
                             snapshots={loadedResultOf(preview.snapshots)} headingLevel={2}/>
        </div>
    </div>;
}
