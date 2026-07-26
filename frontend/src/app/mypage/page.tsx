"use client";

import React from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Grid,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/AuthContext";
import { memberApi } from "@/lib/api";
import { ApiError } from "@/lib/api/errors";
import { useApiData } from "@/lib/useApiData";

export default function MyPage() {
  const { member, refreshMember, logout } = useAuth();
  const router = useRouter();

  const [nickname, setNickname] = React.useState(member?.nickname ?? "");
  const [saving, setSaving] = React.useState(false);
  const [toast, setToast] = React.useState<{ message: string; severity: "success" | "error" } | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [deleting, setDeleting] = React.useState(false);

  const { data: subscription } = useApiData(
    () => (member ? memberApi.getMemberSubscription(member.id).then((r) => r.data) : Promise.resolve(null)),
    [member?.id],
  );

  if (!member) return null;

  const handleSave = async () => {
    setSaving(true);
    try {
      await memberApi.updateMember(member.id, { nickname });
      await refreshMember();
      setToast({ message: "프로필이 저장되었습니다.", severity: "success" });
    } catch (err) {
      setToast({
        message: err instanceof ApiError ? err.message : "저장에 실패했습니다.",
        severity: "error",
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAccount = async () => {
    setDeleting(true);
    try {
      await memberApi.deleteMember(member.id);
      await logout();
      router.push("/");
    } catch (err) {
      setToast({
        message: err instanceof ApiError ? err.message : "회원 탈퇴에 실패했습니다.",
        severity: "error",
      });
      setDeleting(false);
      setDeleteDialogOpen(false);
    }
  };

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        마이페이지
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 5 }}>
          <Card variant="outlined">
            <CardContent sx={{ textAlign: "center", py: 4 }}>
              <Avatar sx={{ width: 72, height: 72, mx: "auto", mb: 2, bgcolor: "primary.main", fontSize: 28 }}>
                {member.nickname[0]}
              </Avatar>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                {member.nickname}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {member.email}
              </Typography>
              {subscription && (
                <Chip
                  label={subscription.currentPlan.name}
                  color={subscription.currentPlan.planTier === "FREE" ? "default" : "secondary"}
                  size="small"
                />
              )}
            </CardContent>
          </Card>

          <Stack spacing={1.5} sx={{ mt: 2 }}>
            <Button component={Link} href="/mypage/subscription" variant="outlined" fullWidth>
              구독 관리
            </Button>
            <Button component={Link} href="/mypage/payments" variant="outlined" fullWidth>
              결제 이력
            </Button>
            <Button variant="text" color="error" fullWidth onClick={() => setDeleteDialogOpen(true)}>
              회원 탈퇴
            </Button>
          </Stack>
        </Grid>

        <Grid size={{ xs: 12, md: 7 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 700 }}>
                프로필 정보
              </Typography>
              <Stack spacing={2.5}>
                <TextField label="닉네임" value={nickname} onChange={(e) => setNickname(e.target.value)} fullWidth />
                <TextField label="이메일" defaultValue={member.email} fullWidth disabled />
                <Divider />
                <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
                  <Button variant="contained" color="secondary" onClick={handleSave} disabled={saving}>
                    {saving ? <CircularProgress size={20} color="inherit" /> : "저장"}
                  </Button>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Dialog open={deleteDialogOpen} onClose={() => !deleting && setDeleteDialogOpen(false)}>
        <DialogTitle sx={{ fontWeight: 700 }}>정말 탈퇴하시겠어요?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            탈퇴 시 계정 정보는 논리 삭제되며, 네이버 연동도 함께 해제됩니다. 이 작업은 되돌릴 수 없습니다.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} color="inherit" disabled={deleting}>
            취소
          </Button>
          <Button onClick={handleDeleteAccount} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : "탈퇴하기"}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={toast !== null} autoHideDuration={4000} onClose={() => setToast(null)}>
        {toast ? (
          <Alert severity={toast.severity} onClose={() => setToast(null)} sx={{ width: "100%" }}>
            {toast.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </Container>
  );
}
