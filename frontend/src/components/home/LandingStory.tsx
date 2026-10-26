import {MousePointerClick, ScanLine, Sparkles} from "lucide-react";
import {Badge} from "@/components/ui/badge";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {Separator} from "@/components/ui/separator";

const FEATURES = [
    {icon: MousePointerClick, title: "반복하던 새로고침은 그만", description: "지원사업, 학사 공지, 공연 예매까지. 같은 페이지를 다시 여는 시간을 줄여보세요."},
    {icon: ScanLine, title: "필요한 변화만 골라서", description: "페이지 전체의 변화부터 키워드의 등장과 사라짐까지. 나에게 필요한 조건을 설정해요."},
    {icon: Sparkles, title: "달라진 내용은 AI 요약으로", description: "무엇이 바뀌었는지 일일이 비교하지 마세요. 중요한 변화를 짧은 요약으로 확인해요."}
] as const;

const STEPS = [
    {number: "01", title: "관심 있는 페이지를 등록해보세요", description: "평소에 놓치고 싶지 않던 페이지가 있으신가요? 와치타워에 등록해보세요!"},
    {number: "02", title: "어떤 변화에 반응할지 설정해요", description: "예매 가능 상태가 되면 알려드릴까요? 아니면 학사 공지가 새로 뜨면 알려드릴까요?"},
    {number: "03", title: "변화가 생기면 알람을 보내드려요", description: "디스코드, 텔레그램 뭐든 좋아요. 앞으로 더 추가될 예정이에요"}
] as const;

export default function LandingStory() {
    return <>
        <section id="how-it-works" className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-6 py-16 md:py-24">
            <div className="flex max-w-2xl flex-col gap-3">
                <h2 className="text-3xl font-semibold tracking-tight break-keep">소식을 기다리며 새로고침만 하기엔, 우리 삶엔 할 일이 너무
                    많으니까요</h2>
                <p>2026년 서울시 청년지원금을 놓치고
                    "누가 나 대신 사이트 좀 지켜봐 줬으면" 하는 마음이 들어 개발하게 되었습니다.

                    새로고침은 와치타워가 할 테니, 여러분은 일상에 집중하세요!</p>
            </div>
            <div className="grid gap-6 md:grid-cols-3">
                {FEATURES.map(({icon: Icon, title, description}) => <Card key={title}>
                    <CardHeader className="gap-4"><Icon className="size-5"
                                                        aria-hidden="true"/><CardTitle>{title}</CardTitle></CardHeader>
                    <CardContent><CardDescription>{description}</CardDescription></CardContent>
                </Card>)}
            </div>
        </section>
        <Separator/>
        <section
            className="mx-auto flex w-full max-w-6xl scroll-mt-20 flex-col gap-8 px-6 py-16 md:py-24">
            <div className="flex max-w-2xl flex-col gap-3">
                <h2 className="text-3xl font-semibold tracking-tight break-keep">추가하고 기다리면 돼요. 간단하죠?</h2>
            </div>
            <ol className="grid gap-6 md:grid-cols-3">
                {STEPS.map(step => <li key={step.number}><Card className="h-full">
                    <CardHeader className="gap-4"><Badge
                        variant="secondary">{step.number}</Badge><CardTitle>{step.title}</CardTitle></CardHeader>
                    <CardContent><CardDescription>{step.description}</CardDescription></CardContent>
                </Card></li>)}
            </ol>
        </section>
        <Separator/>
    </>;
}
