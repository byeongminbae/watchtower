"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ChevronDown, LogOut, Radar, Settings, ShieldCheck, UserRound, UsersRound, X, type LucideIcon } from "lucide-react";
import LighthouseMark from "@/components/common/LighthouseMark";
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button, buttonVariants } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { useAuth } from "@/lib/AuthContext";
import { isRouteOf } from "@/lib/navigation";

type NavigationItem = { readonly href: string; readonly label: string; readonly icon: LucideIcon };
const memberNavigation: readonly NavigationItem[] = [
  { href: "/watches", label: "내 타워", icon: Radar },
  { href: "/mypage", label: "마이페이지", icon: UserRound }
];
const adminNavigation: NavigationItem = { href: "/admin", label: "관리자", icon: ShieldCheck };

function MainNavigation({ items, pathname }: { readonly items: readonly NavigationItem[]; readonly pathname: string }) {
  return <nav aria-label="주 메뉴" className="flex items-center gap-1">
    {items.map(({ href, label, icon: Icon }) => {
      const active = isRouteOf(pathname, href);
      return <Link key={href} href={href} aria-label={label} aria-current={active ? "page" : undefined}
        className={buttonVariants({ variant: active ? "secondary" : "ghost" })}>
        <Icon data-icon="inline-start" /><span className="hidden sm:inline">{label}</span>
      </Link>;
    })}
  </nav>;
}

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
    <header className="sticky top-0 z-30 border-b bg-background/95 backdrop-blur">
      <div className="flex h-16 items-center justify-between gap-3 px-4 sm:px-6">
        <div className="flex min-w-0 items-center gap-2 sm:gap-6">
          <Link href="/" className="flex shrink-0 items-center gap-2 font-semibold"><LighthouseMark size={28} /><span>Watchtower</span></Link>
          {isLoggedIn && <MainNavigation pathname={pathname}
            items={principal?.role === "ADMIN" ? [...memberNavigation, adminNavigation] : memberNavigation} />}
        </div>
        <div className="flex min-w-0 items-center gap-2">
          {isLoggedIn ? <>
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
