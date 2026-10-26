"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, LayoutDashboard, Radar, ShieldCheck, UserRound, UsersRound, type LucideIcon } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { useAuth } from "@/lib/AuthContext";
import { cn } from "@/lib/utils";

type NavigationItem = { readonly href: string; readonly label: string; readonly icon: LucideIcon };
const memberNavigation: readonly NavigationItem[] = [
  { href: "/watches", label: "내 타워", icon: Radar },
  { href: "/mypage/notifications", label: "알림 채널", icon: Bell },
  { href: "/mypage", label: "마이페이지", icon: UserRound }
];
const adminNavigation: readonly NavigationItem[] = [
  { href: "/admin", label: "관리자", icon: LayoutDashboard },
  { href: "/admin/members", label: "회원 관리", icon: UsersRound },
  { href: "/admin/watches", label: "타워 관리", icon: ShieldCheck }
];

function NavigationLinks({ items, pathname }: { readonly items: readonly NavigationItem[]; readonly pathname: string }) {
  return items.map(({ href, label, icon: Icon }) => {
    const active = pathname === href || (href === "/watches" && pathname.startsWith("/watches/"));
    return <Link key={href} href={href} className={cn(buttonVariants({ variant: active ? "secondary" : "ghost" }), "justify-start gap-2")} aria-current={active ? "page" : undefined}>
      <Icon data-icon="inline-start" /><span>{label}</span>
    </Link>;
  });
}

export default function AppShell({ children }: { readonly children: ReactNode }) {
  const pathname = usePathname();
  const { isLoggedIn, principal } = useAuth();
  const isWorkspace = ["/watches", "/mypage", "/admin"].some(route => pathname === route || pathname.startsWith(`${route}/`));
  if (!isLoggedIn || !isWorkspace) return <div id="main-content" tabIndex={-1}>{children}</div>;
  const isAdmin = principal?.role === "ADMIN";
  return <div className="mx-auto grid min-h-[calc(100dvh-4rem)] max-w-screen-2xl md:grid-cols-[208px_minmax(0,1fr)]">
    <aside className="sticky top-16 hidden h-[calc(100dvh-4rem)] flex-col gap-4 border-r bg-sidebar p-4 md:flex">
      <p className="px-2 text-xs text-muted-foreground">워크스페이스</p>
      <nav className="flex flex-col gap-1" aria-label="내 메뉴"><NavigationLinks items={memberNavigation} pathname={pathname} /></nav>
      {isAdmin && <><Separator /><p className="px-2 text-xs text-muted-foreground">관리</p><nav className="flex flex-col gap-1" aria-label="관리자 메뉴"><NavigationLinks items={adminNavigation} pathname={pathname} /></nav></>}
    </aside>
    <div className="min-w-0" id="main-content" tabIndex={-1}>
      <nav className="flex flex-wrap gap-1 border-b p-3 md:hidden" aria-label="페이지 메뉴"><NavigationLinks items={isAdmin ? [...memberNavigation, ...adminNavigation] : memberNavigation} pathname={pathname} /></nav>
      {children}
    </div>
  </div>;
}
