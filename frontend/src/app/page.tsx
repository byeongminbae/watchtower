"use client";

import Link from "next/link";
import {ArrowRight, Bell, Sparkles} from "lucide-react";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert";
import {Badge} from "@/components/ui/badge";
import {buttonVariants} from "@/components/ui/button";
import {Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from "@/components/ui/card";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table";
import LandingStory from "@/components/home/LandingStory";
import HotUrlSection from "@/components/watch/HotUrlSection";
import {useAuth} from "@/lib/AuthContext";

export default function HomePage() {
    const {isLoggedIn} = useAuth();
    const startPath = isLoggedIn ? "/watches" : "/login";
    return <main>
        <section className="mx-auto flex max-w-4xl flex-col items-center gap-6 px-6 py-16 text-center md:py-24">
            <h1 className="text-4xl font-semibold tracking-tight break-keep md:text-6xl">변화를 기다리지 말고 받아보세요</h1>
            <p className="max-w-2xl text-base text-muted-foreground break-keep md:text-lg">새로고침 하지 않아도 와치타워가 대신
                지켜보고, <br/>
                달라진 내용을 AI로 요약해 알림을 보내드려요.</p>
            <div className="flex flex-wrap justify-center gap-3">
                <Link className={buttonVariants({size: "lg"})} href={startPath}>지금 감시 시작하기<ArrowRight
                    data-icon="inline-end"/></Link>
                <a className={buttonVariants({size: "lg", variant: "outline"})} href="#how-it-works">어떤 서비스인지 궁금해요!</a>
            </div>
            <p className="text-xs text-muted-foreground">채용 공고부터 학사 공지, 예매 소식까지 뭐든지 좋아요!</p>
        </section>
        <section className="mx-auto w-full max-w-5xl px-6 pb-16" aria-label="타워 변화 감지 사용 예시">
            <Card>
                <CardHeader><CardTitle>페이지 변화 모니터링</CardTitle><CardDescription>등록한 페이지와 최근 변화를 한곳에서
                    확인해요.</CardDescription><CardAction><Badge variant="outline">사용 예시</Badge></CardAction></CardHeader>
                <CardContent className="flex flex-col gap-6">
                    <Table><TableHeader><TableRow><TableHead>타워</TableHead><TableHead>감지 조건</TableHead><TableHead>알림
                        채널</TableHead></TableRow></TableHeader>
                        <TableBody><TableRow><TableCell><p className="font-medium">서울대학교 학사 공지 알림</p><p
                            className="text-xs text-muted-foreground">snu.ac.kr</p>
                        </TableCell><TableCell><Badge variant="secondary">키워드
                            감지</Badge></TableCell><TableCell>Discord</TableCell></TableRow></TableBody>
                    </Table>
                    <Alert><Sparkles/><AlertTitle>AI 변화 요약</AlertTitle><AlertDescription>2027년 봄 조기졸업 신청 안내가 추가됐어요. 신청
                        일정을 확인해 보세요.</AlertDescription></Alert>
                </CardContent>
                <CardFooter className="gap-2"><Bell className="size-4" aria-hidden="true"/><p
                    className="text-xs text-muted-foreground">기능을 설명하기 위한 예시예요. 실제 감지 결과가 아니에요.</p></CardFooter>
            </Card>
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
