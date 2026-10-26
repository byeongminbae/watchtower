"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Bell, ChevronDown, LogOut, Settings, ShieldCheck, UsersRound, X } from "lucide-react";
import LighthouseMark from "@/components/common/LighthouseMark";
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button, buttonVariants } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { useAuth } from "@/lib/AuthContext";
import { cn } from "@/lib/utils";

export default function Header() {
  const { isLoggedIn, principal, member, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const name = member?.nickname.trim() || "회원";
  async function signOut() {
    setMenuOpen(false);
    setError(null);
    try {
      await logout();
      router.push("/");
    } catch (reason) {
      if (!(reason instanceof Error)) throw reason;
      setError(reason.message);
    }
  }
  return <>
    <header className="sticky top-0 z-20 border-b bg-background">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-3 px-4 sm:px-6">
        <Link href="/" className="flex shrink-0 items-center gap-2 font-semibold"><LighthouseMark size={28} /><span>Watchtower</span></Link>
        <div className="flex min-w-0 items-center gap-2">
          {isLoggedIn ? <>
            <Link href="/watches" className={cn(buttonVariants({ variant: "ghost" }), "hidden sm:inline-flex")}
              aria-current={pathname.startsWith("/watches") ? "page" : undefined}>내 타워</Link>
            {principal?.role === "ADMIN" && <Link href="/admin" className={cn(buttonVariants({ variant: "ghost" }), "hidden sm:inline-flex")}
              aria-current={pathname.startsWith("/admin") ? "page" : undefined}>관리자</Link>}

            <DropdownMenu open={menuOpen} onOpenChange={setMenuOpen}>
              <DropdownMenuTrigger render={<Button variant="ghost" />} aria-label={`${name} 메뉴`}>
                <Avatar className="size-6"><AvatarImage src={member?.profileImageUrl || undefined} alt={`${name} 프로필 사진`} /><AvatarFallback>{name[0]}</AvatarFallback></Avatar>
                <span className="hidden max-w-36 truncate sm:inline">{name}</span>
                <ChevronDown data-icon="inline-end" />
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" sideOffset={10} className="min-w-52">
                <DropdownMenuGroup>
                  <DropdownMenuLabel>내 계정</DropdownMenuLabel>
                  <DropdownMenuItem render={<Link href="/mypage" />}><Settings />마이페이지</DropdownMenuItem>
                  <DropdownMenuItem render={<Link href="/mypage/notifications" />}><Bell />알림 채널</DropdownMenuItem>
                </DropdownMenuGroup>
                {principal?.role === "ADMIN" && <>
                  <DropdownMenuSeparator />
                  <DropdownMenuGroup>
                    <DropdownMenuLabel>관리</DropdownMenuLabel>
                    <DropdownMenuItem render={<Link href="/admin/members" />}><UsersRound />회원 관리</DropdownMenuItem>
                    <DropdownMenuItem render={<Link href="/admin/watches" />}><ShieldCheck />타워 관리</DropdownMenuItem>
                  </DropdownMenuGroup>
                </>}
                <DropdownMenuSeparator />
                <DropdownMenuGroup><DropdownMenuItem onClick={() => void signOut()}><LogOut />로그아웃</DropdownMenuItem></DropdownMenuGroup>
              </DropdownMenuContent>
            </DropdownMenu>
          </> : <Link href="/login" className={buttonVariants()}>로그인</Link>}
        </div>
      </div>
    </header>
    {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription><AlertAction><Button size="icon" variant="ghost" aria-label="오류 닫기" onClick={() => setError(null)}><X /></Button></AlertAction></Alert>}
  </>;
}
