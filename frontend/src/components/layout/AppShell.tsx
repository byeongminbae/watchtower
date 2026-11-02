"use client";

import { Fragment, type CSSProperties, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, LayoutDashboard, Radar, ShieldCheck, UserRound, UsersRound, type LucideIcon } from "lucide-react";
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from "@/components/ui/breadcrumb";
import { Separator } from "@/components/ui/separator";
import {
  Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarMenu, SidebarMenuButton, SidebarMenuItem,
  SidebarProvider, SidebarRail, SidebarTrigger
} from "@/components/ui/sidebar";
import { useAuth } from "@/lib/AuthContext";

type NavigationItem = { readonly href: string; readonly label: string; readonly icon: LucideIcon };
const memberNavigation: readonly NavigationItem[] = [
  { href: "/watches", label: "내 타워", icon: Radar },
  { href: "/mypage/notifications", label: "알림 채널", icon: Bell },
  { href: "/mypage", label: "마이페이지", icon: UserRound }
];
const adminNavigation: readonly NavigationItem[] = [
  { href: "/admin", label: "대시보드", icon: LayoutDashboard },
  { href: "/admin/members", label: "회원 관리", icon: UsersRound },
  { href: "/admin/watches", label: "타워 관리", icon: ShieldCheck }
];

/** 경로마다 상단 바에 보여줄 위치. 마지막 항목이 현재 화면 */
function breadcrumbsOf(pathname: string): readonly { readonly label: string; readonly href?: string }[] {
  if (/^\/watches\/[^/]+$/.test(pathname)) return [{ label: "내 타워", href: "/watches" }, { label: "타워 상세" }];
  if (pathname.startsWith("/watches")) return [{ label: "내 타워" }];
  if (pathname.startsWith("/mypage/notifications")) return [{ label: "마이페이지", href: "/mypage" }, { label: "알림 채널" }];
  if (pathname.startsWith("/mypage")) return [{ label: "마이페이지" }];
  if (pathname.startsWith("/admin/members")) return [{ label: "관리자", href: "/admin" }, { label: "회원 관리" }];
  if (pathname.startsWith("/admin/watches")) return [{ label: "관리자", href: "/admin" }, { label: "타워 관리" }];
  return [{ label: "관리자" }, { label: "대시보드" }];
}

function NavigationGroup({ label, items, pathname }: { readonly label: string; readonly items: readonly NavigationItem[]; readonly pathname: string }) {
  return <SidebarGroup>
    <SidebarGroupLabel>{label}</SidebarGroupLabel>
    <SidebarGroupContent>
      <SidebarMenu aria-label={`${label} 메뉴`}>
        {items.map(({ href, label: itemLabel, icon: Icon }) => {
          const active = pathname === href || (href === "/watches" && pathname.startsWith("/watches/"));
          return <SidebarMenuItem key={href}>
            <SidebarMenuButton render={<Link href={href} aria-current={active ? "page" : undefined} />} isActive={active} tooltip={itemLabel}>
              <Icon /><span>{itemLabel}</span>
            </SidebarMenuButton>
          </SidebarMenuItem>;
        })}
      </SidebarMenu>
    </SidebarGroupContent>
  </SidebarGroup>;
}

export default function AppShell({ children }: { readonly children: ReactNode }) {
  const pathname = usePathname();
  const { isLoggedIn, principal } = useAuth();
  const isWorkspace = ["/watches", "/mypage", "/admin"].some(route => pathname === route || pathname.startsWith(`${route}/`));
  if (!isLoggedIn || !isWorkspace) return <div id="main-content" tabIndex={-1}>{children}</div>;
  const isAdmin = principal?.role === "ADMIN";
  const breadcrumbs = breadcrumbsOf(pathname);

  return <SidebarProvider style={{ "--header-height": "4rem" } as CSSProperties} className="min-h-[calc(100svh-var(--header-height))]">
    <Sidebar collapsible="icon" className="top-(--header-height) h-[calc(100svh-var(--header-height))]!">
      <SidebarContent>
        <NavigationGroup label="워크스페이스" items={memberNavigation} pathname={pathname} />
        {isAdmin && <NavigationGroup label="관리" items={adminNavigation} pathname={pathname} />}
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
    <div className="flex min-w-0 flex-1 flex-col bg-muted/40" id="main-content" tabIndex={-1}>
      <div className="sticky top-(--header-height) z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background/95 px-4 backdrop-blur">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-1 data-[orientation=vertical]:h-4" />
        <Breadcrumb>
          <BreadcrumbList>
            {breadcrumbs.map((crumb, index) => <Fragment key={crumb.label}>
              {index > 0 && <BreadcrumbSeparator />}
              <BreadcrumbItem>
                {crumb.href ? <BreadcrumbLink render={<Link href={crumb.href} />}>{crumb.label}</BreadcrumbLink> : <BreadcrumbPage>{crumb.label}</BreadcrumbPage>}
              </BreadcrumbItem>
            </Fragment>)}
          </BreadcrumbList>
        </Breadcrumb>
      </div>
      {children}
    </div>
  </SidebarProvider>;
}
