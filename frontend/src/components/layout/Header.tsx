"use client";

import Link from "next/link";
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Container,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from "@mui/material";
import { useRouter } from "next/navigation";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import React from "react";
import LighthouseMark from "@/components/common/LighthouseMark";
import { useAuth } from "@/lib/AuthContext";

export default function Header() {
  const { isLoggedIn, principal, member, logout } = useAuth();
  const router = useRouter();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);
  const isAdmin = principal?.role === "ADMIN";
  const displayName = member?.nickname.trim() || "회원";

  const handleLogout = async () => {
    setAnchorEl(null);
    await logout();
    router.push("/");
  };

  const navItems = isAdmin
    ? [{ label: "관리자", href: "/admin" }]
    : [{ label: "내 와치리스트", href: "/watches" }];

  return (
    <AppBar position="sticky" elevation={0}>
      <Container maxWidth="lg">
        <Toolbar disableGutters sx={{ gap: 3 }}>
          <Box
            component={Link}
            href="/"
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1,
              textDecoration: "none",
              color: "inherit",
            }}
          >
            <LighthouseMark size={26} />
            <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: -0.5 }}>
              Watchtower
            </Typography>
          </Box>

          {isLoggedIn && (
            <Box sx={{ display: "flex", gap: 0.5, flexGrow: 1 }}>
              {navItems.map((item) => (
                <Button
                  key={item.href}
                  component={Link}
                  href={item.href}
                  color="inherit"
                  sx={{ color: "text.secondary", "&:hover": { color: "text.primary" } }}
                >
                  {item.label}
                </Button>
              ))}
            </Box>
          )}
          {!isLoggedIn && <Box sx={{ flexGrow: 1 }} />}

          {isLoggedIn ? (
            <>
              {!isAdmin && (
                <Button
                  component={Link}
                  href="/watches"
                  variant="contained"
                  color="secondary"
                  size="small"
                  sx={{ display: { xs: "none", sm: "inline-flex" } }}
                >
                  와치 등록
                </Button>
              )}

              <Box
                component="button"
                type="button"
                aria-label={`${displayName} 메뉴`}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 0.5,
                  p: 0,
                  border: 0,
                  bgcolor: "transparent",
                  color: "inherit",
                  cursor: "pointer",
                }}
                onClick={(e) => setAnchorEl(e.currentTarget)}
              >
                <Avatar sx={{ width: 32, height: 32, bgcolor: "primary.main" }}>
                  {isAdmin ? <AdminPanelSettingsOutlinedIcon fontSize="small" /> : displayName[0]}
                </Avatar>
                <KeyboardArrowDownIcon fontSize="small" sx={{ color: "text.secondary" }} />
              </Box>
              <Menu anchorEl={anchorEl} open={open} onClose={() => setAnchorEl(null)}>
                {isAdmin
                  ? [
                      <MenuItem
                        key="admin-dashboard"
                        component={Link}
                        href="/admin"
                        onClick={() => setAnchorEl(null)}
                      >
                        대시보드
                      </MenuItem>,
                      <MenuItem
                        key="admin-watches"
                        component={Link}
                        href="/admin/watches"
                        onClick={() => setAnchorEl(null)}
                      >
                        와치리스트 관리
                      </MenuItem>,
                      <MenuItem
                        key="admin-payments"
                        component={Link}
                        href="/admin/payments"
                        onClick={() => setAnchorEl(null)}
                      >
                        결제 내역 관리
                      </MenuItem>,
                      <MenuItem
                        key="admin-users"
                        component={Link}
                        href="/admin/users"
                        onClick={() => setAnchorEl(null)}
                      >
                        유저 관리
                      </MenuItem>,
                    ]
                  : [
                      <MenuItem key="mypage" component={Link} href="/mypage" onClick={() => setAnchorEl(null)}>
                        마이페이지
                      </MenuItem>,
                      <MenuItem
                        key="subscription"
                        component={Link}
                        href="/mypage/subscription"
                        onClick={() => setAnchorEl(null)}
                      >
                        구독 관리
                      </MenuItem>,
                      <MenuItem
                        key="payments"
                        component={Link}
                        href="/mypage/payments"
                        onClick={() => setAnchorEl(null)}
                      >
                        결제 이력
                      </MenuItem>,
                    ]}
                <MenuItem onClick={handleLogout}>로그아웃</MenuItem>
              </Menu>
            </>
          ) : (
            <Button component={Link} href="/login" variant="contained" color="secondary">
              로그인
            </Button>
          )}
        </Toolbar>
      </Container>
    </AppBar>
  );
}
