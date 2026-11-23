"use client";

import { Fragment, type CSSProperties, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, ShieldCheck, UsersRound, type LucideIcon } from "lucide-react";
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from "@/components/ui/breadcrumb";
import { Separator } from "@/components/ui/separator";
import {
  Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarMenu, SidebarMenuButton, SidebarMenuItem,
  SidebarProvider, SidebarRail, SidebarTrigger
} from "@/components/ui/sidebar";
import { useAuth } from "@/lib/AuthContext";
import { isRouteOf } from "@/lib/navigation";

type NavigationItem = { readonly href: string; readonly label: string; readonly icon: LucideIcon };
const adminNavigation: readonly NavigationItem[] = [
  { href: "/admin", label: "대시보드", icon: LayoutDashboard },
  { href: "/admin/members", label: "회원 관리", icon: UsersRound },
  { href: "/admin/watches", label: "타워 관리", icon: ShieldCheck }
];
const headerHeight = { "--header-height": "4rem" } as CSSProperties;

/** 경로마다 상단 바에 보여줄 위치. 마지막 항목이 현재 화면 */
function breadcrumbsOf(pathname: string): readonly { readonly label: string; readonly href?: string }[] {
  if (/^\/watches\/[^/]+$/.test(pathname)) return [{ label: "내 타워", href: "/watches" }, { label: "타워 상세" }];
  if (pathname.startsWith("/watches")) return [{ label: "내 타워" }];
  if (pathname.startsWith("/mypage")) return [{ label: "마이페이지" }];
  if (pathname.startsWith("/admin/members")) return [{ label: "관리자", href: "/admin" }, { label: "회원 관리" }];
  if (pathname.startsWith("/admin/watches")) return [{ label: "관리자", href: "/admin" }, { label: "타워 관리" }];
  return [{ label: "관리자" }, { label: "대시보드" }];
}

function PageLocationBar({ pathname, children }: { readonly pathname: string; readonly children?: ReactNode }) {
  return <div className="sticky top-(--header-height) z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background/95 px-4 backdrop-blur">
    {children}
    <Breadcrumb>
      <BreadcrumbList>
        {breadcrumbsOf(pathname).map((crumb, index) => <Fragment key={crumb.label}>
          {index > 0 && <BreadcrumbSeparator />}
          <BreadcrumbItem>
            {crumb.href ? <BreadcrumbLink render={<Link href={crumb.href} />}>{crumb.label}</BreadcrumbLink> : <BreadcrumbPage>{crumb.label}</BreadcrumbPage>}
          </BreadcrumbItem>
        </Fragment>)}
      </BreadcrumbList>
    </Breadcrumb>
  </div>;
}

function AdminNavigation({ pathname }: { readonly pathname: string }) {
  return <SidebarGroup>
    <SidebarGroupLabel>관리</SidebarGroupLabel>
    <SidebarGroupContent>
      <SidebarMenu aria-label="관리 메뉴">
        {adminNavigation.map(({ href, label, icon: Icon }) => {
          const active = pathname === href;
          return <SidebarMenuItem key={href}>
            <SidebarMenuButton render={<Link href={href} aria-current={active ? "page" : undefined} />} isActive={active} tooltip={label}>
              <Icon /><span>{label}</span>
            </SidebarMenuButton>
          </SidebarMenuItem>;
        })}
      </SidebarMenu>
    </SidebarGroupContent>
  </SidebarGroup>;
}

export default function AppShell({ children }: { readonly children: ReactNode }) {
  const pathname = usePathname();
  const { isLoggedIn } = useAuth();
  const isWorkspace = ["/watches", "/mypage", "/admin"].some(route => isRouteOf(pathname, route));
  if (!isLoggedIn || !isWorkspace) return <div id="main-content" tabIndex={-1}>{children}</div>;

  if (!isRouteOf(pathname, "/admin")) {
    return <div style={headerHeight} className="flex min-h-[calc(100svh-var(--header-height))] min-w-0 flex-col bg-muted/40" id="main-content" tabIndex={-1}>
      <PageLocationBar pathname={pathname} />
      {children}
    </div>;
  }

  return <SidebarProvider style={headerHeight} className="min-h-[calc(100svh-var(--header-height))]">
    <Sidebar collapsible="icon" className="top-(--header-height) h-[calc(100svh-var(--header-height))]!">
      <SidebarContent>
        <AdminNavigation pathname={pathname} />
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
    <div className="flex min-w-0 flex-1 flex-col bg-muted/40" id="main-content" tabIndex={-1}>
      <PageLocationBar pathname={pathname}>
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-1 data-[orientation=vertical]:h-4" />
      </PageLocationBar>
      {children}
    </div>
  </SidebarProvider>;
}
