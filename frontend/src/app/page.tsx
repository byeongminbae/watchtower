"use client";

import dynamic from "next/dynamic";
import Link from "next/link";
import {ArrowRight} from "lucide-react";
import {buttonVariants} from "@/components/ui/button";
import LandingStory from "@/components/home/LandingStory";
import HotUrlSection from "@/components/watch/HotUrlSection";
import {Skeleton} from "@/components/ui/skeleton";
import {useAuth} from "@/lib/AuthContext";

const WatchDetailPreview = dynamic(() => import("@/components/home/WatchDetailPreview"), {
    ssr: false,
    loading: () => <Skeleton className="h-[80rem] w-full rounded-2xl md:h-[59rem]"/>
});

export default function HomePage() {
    const {isLoggedIn} = useAuth();
    const startPath = isLoggedIn ? "/watches" : "/login";
    return <main>
        <section className="mx-auto grid w-full max-w-7xl items-center gap-12 px-6 py-16 xl:py-12 xl:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
            <div className="flex flex-col items-center gap-6 text-center xl:items-start xl:text-left">
                <h1 className="text-4xl font-semibold tracking-tight break-keep md:text-6xl">변화를 기다리지 말고 받아보세요</h1>
                <p className="max-w-2xl text-base text-muted-foreground break-keep md:text-lg">새로고침 하지 않아도 와치타워가 대신
                    지켜보고, <br/>
                    달라진 내용을 AI로 요약해 알림을 보내드려요.</p>
                <div className="flex flex-wrap justify-center gap-3 xl:justify-start">
                    <Link className={buttonVariants({size: "lg"})} href={startPath}>지금 감시 시작하기<ArrowRight
                        data-icon="inline-end"/></Link>
                    <a className={buttonVariants({size: "lg", variant: "outline"})} href="#how-it-works">어떤 서비스인지 궁금해요!</a>
                </div>
                <p className="text-xs text-muted-foreground">채용 공고부터 학사 공지, 예매 소식까지 뭐든지 좋아요!</p>
            </div>
            <WatchDetailPreview/>
        </section>
        <HotUrlSection/>
        <LandingStory/>
        <section className="mx-auto flex max-w-3xl flex-col items-center gap-6 px-6 py-16 text-center md:py-24">
            <h2 className="text-3xl font-semibold tracking-tight break-keep">기다림은 줄이고,<br/>필요한 소식만 받아보세요.</h2>
            <p className="text-muted-foreground">지금 가장 기다리는 페이지 하나부터 등록해 보세요.</p>
            <Link className={buttonVariants({size: "lg"})} href={startPath}>첫 타워 만들기<ArrowRight data-icon="inline-end"/></Link>
        </section>
        <footer className="border-t">
            <div
                className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-6 text-xs text-muted-foreground">
                <span>Watchtower</span><span>당신이 기다리는 변화를 대신 지켜봐요.</span></div>
        </footer>
    </main>;
}
