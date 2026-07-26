"use client";

import React from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Menu,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import InputAdornment from "@mui/material/InputAdornment";
import SearchIcon from "@mui/icons-material/Search";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import { ApiError } from "@/lib/api/errors";
import { Member, MemberRole } from "@/types/domain";

export default function AdminUsersPage() {
  const [search, setSearch] = React.useState("");
  const [roleMenuAnchor, setRoleMenuAnchor] = React.useState<null | HTMLElement>(null);
  const [targetUser, setTargetUser] = React.useState<Member | null>(null);
  const [banTarget, setBanTarget] = React.useState<Member | null>(null);
  const [updating, setUpdating] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);

  const { data: users, loading, error, refetch } = useApiData(
    () => adminApi.getUsers().then((r) => r.data),
    [],
  );

  const filtered = (users ?? []).filter(
    (u) => u.nickname.toLowerCase().includes(search.toLowerCase()) || u.email.toLowerCase().includes(search.toLowerCase()),
  );

  const handleOpenRoleMenu = (e: React.MouseEvent<HTMLElement>, user: Member) => {
    setRoleMenuAnchor(e.currentTarget);
    setTargetUser(user);
  };

  const handleChangeRole = async (role: MemberRole) => {
    if (!targetUser) return;
    setRoleMenuAnchor(null);
    setUpdating(true);
    setActionError(null);
    try {
      await adminApi.updateUserRole(targetUser.id, role);
      refetch();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "권한 변경에 실패했습니다.");
    } finally {
      setUpdating(false);
      setTargetUser(null);
    }
  };

  const handleToggleBan = async () => {
    if (!banTarget) return;
    setUpdating(true);
    setActionError(null);
    try {
      await adminApi.updateUserStatus(banTarget.id, !banTarget.isBanned);
      refetch();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setUpdating(false);
      setBanTarget(null);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        유저 관리
      </Typography>

      {actionError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      <TextField
        size="small"
        placeholder="닉네임, 이메일로 검색"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 2, width: 320 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon fontSize="small" />
            </InputAdornment>
          ),
        }}
      />

      {loading && (
        <Box sx={{ display: "flex", justifyContent: "center", py: 10 }}>
          <CircularProgress color="secondary" />
        </Box>
      )}

      {!loading && error && <Alert severity="error">{error}</Alert>}

      {!loading && !error && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>유저</TableCell>
                <TableCell>이메일</TableCell>
                <TableCell>권한</TableCell>
                <TableCell>상태</TableCell>
                <TableCell>최근 로그인</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((user) => (
                <TableRow key={user.id} hover>
                  <TableCell>
                    <Stack direction="row" spacing={1.5} alignItems="center">
                      <Avatar sx={{ width: 28, height: 28, bgcolor: "primary.main", fontSize: 13 }}>
                        {user.nickname[0]}
                      </Avatar>
                      <Typography variant="body2">{user.nickname}</Typography>
                    </Stack>
                  </TableCell>
                  <TableCell sx={{ color: "text.secondary" }}>{user.email}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={user.role}
                      color={user.role === "ADMIN" ? "secondary" : "default"}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    {user.isBanned && <Chip size="small" label="정지됨" color="error" variant="outlined" />}
                  </TableCell>
                  <TableCell>{new Date(user.lastLoginAt).toLocaleDateString("ko-KR")}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      sx={{ mr: 1 }}
                      onClick={(e) => handleOpenRoleMenu(e, user)}
                      disabled={updating}
                    >
                      권한 변경
                    </Button>
                    <Button size="small" color="error" onClick={() => setBanTarget(user)} disabled={updating}>
                      {user.isBanned ? "정지 해제" : "Ban"}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6, color: "text.secondary" }}>
                    조회된 유저가 없습니다.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Menu anchorEl={roleMenuAnchor} open={Boolean(roleMenuAnchor)} onClose={() => setRoleMenuAnchor(null)}>
        <MenuItem onClick={() => handleChangeRole("USER")}>USER로 변경</MenuItem>
        <MenuItem onClick={() => handleChangeRole("ADMIN")}>ADMIN으로 변경</MenuItem>
      </Menu>

      <Dialog open={banTarget !== null} onClose={() => !updating && setBanTarget(null)}>
        <DialogTitle sx={{ fontWeight: 700 }}>
          {banTarget?.isBanned ? "정지를 해제할까요?" : "이 유저를 정지할까요?"}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {banTarget?.nickname} 님을 {banTarget?.isBanned ? "다시 활성화" : "정지"}합니다.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setBanTarget(null)} color="inherit" disabled={updating}>
            취소
          </Button>
          <Button onClick={handleToggleBan} color="error" variant="contained" disabled={updating}>
            {updating ? <CircularProgress size={20} color="inherit" /> : "확인"}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
