import {MousePointerClick, ScanLine, Sparkles} from "lucide-react";
import {Badge} from "@/components/ui/badge";
import {Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {Separator} from "@/components/ui/separator";

const STEPS = [
    {number: "01", icon: MousePointerClick, title: "자꾸 새로고침하던 페이지를 등록해요", description: "지원사업, 학사 공지, 공연 예매까지. 같은 페이지를 다시 여는 일은 와치타워가 대신해요."},
    {number: "02", icon: ScanLine, title: "어떤 변화에 알릴지 골라요", description: "글자와 이미지의 변화부터 키워드의 등장과 사라짐까지. 원하는 영역만 골라서 지켜볼 수도 있어요."},
    {number: "03", icon: Sparkles, title: "달라진 내용을 AI 요약으로 받아요", description: "바뀐 곳을 일일이 비교하지 않아도 돼요. 짧은 요약과 바뀐 곳을 표시한 스크린샷을 디스코드로 보내드려요."}
] as const;

export default function LandingStory() {
    return <>
        <section id="how-it-works" className="mx-auto flex w-full max-w-6xl scroll-mt-20 flex-col gap-8 px-6 py-16 md:py-24">
            <div className="flex max-w-2xl flex-col gap-3">
                <h2 className="text-3xl font-semibold tracking-tight break-keep">소식을 기다리며 새로고침만 하기엔, 우리 삶엔 할 일이 너무
                    많으니까요</h2>
                <p>2026년 서울시 청년지원금을 놓치고
                    &quot;누가 나 대신 사이트 좀 지켜봐 줬으면&quot; 하는 마음이 들어 만들었어요.
                    등록하고 기다리기만 하면 돼요. 새로고침은 와치타워가 할 테니, 여러분은 일상에 집중하세요!</p>
            </div>
            <ol className="grid gap-6 md:grid-cols-3">
                {STEPS.map(({number, icon: Icon, title, description}) => <li key={number}><Card className="h-full">
                    <CardHeader className="gap-4">
                        <Icon className="size-5" aria-hidden="true"/>
                        <CardTitle>{title}</CardTitle>
                        <CardAction><Badge variant="secondary">{number}</Badge></CardAction>
                    </CardHeader>
                    <CardContent><CardDescription>{description}</CardDescription></CardContent>
                </Card></li>)}
            </ol>
        </section>
        <Separator/>
    </>;
}
