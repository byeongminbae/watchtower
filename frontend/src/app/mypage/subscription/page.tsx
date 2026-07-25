"use client";

import React from "react";
import {
  Alert,
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
  Typography,
} from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/AuthContext";
import { memberApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import { ApiError } from "@/lib/api/errors";

// 스웨거에 플랜 목록 조회 엔드포인트가 없어, 클라이언트에서 알고 있는 고정 플랜 정보로 안내한다.
// (요구사항 기준: Free/Basic 두 플랜만 존재. 실제 구매는 /payments/toss/confirm 흐름을 따로 구현 필요)
const KNOWN_PLANS = [
  { name: "Free", price: 0, features: ["와치리스트 최대 10개", "커뮤니티 지원"] },
  { name: "Basic", price: 1000, features: ["와치리스트 최대 20개", "우선 지원", "언제든 해지 가능"] },
];

export default function SubscriptionPage() {
  const { member } = useAuth();
  const router = useRouter();
  const [cancelDialogOpen, setCancelDialogOpen] = React.useState(false);
  const [cancelling, setCancelling] = React.useState(false);
  const [cancelError, setCancelError] = React.useState<string | null>(null);

  const {
    data: subscription,
    loading,
    error,
    refetch,
  } = useApiData(
    () => (member ? memberApi.getMemberSubscription(member.id).then((r) => r.data) : Promise.resolve(null)),
    [member?.id],
  );

  const handleCancel = async () => {
    if (!member) return;
    setCancelling(true);
    setCancelError(null);
    try {
      await memberApi.cancelMemberSubscription(member.id);
      setCancelDialogOpen(false);
      refetch();
    } catch (err) {
      setCancelError(err instanceof ApiError ? err.message : "구독 해지에 실패했습니다.");
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
        <CircularProgress color="secondary" />
      </Box>
    );
  }

  if (error || !subscription) {
    return (
      <Container maxWidth="sm" sx={{ py: 10 }}>
        <Alert severity="error">{error ?? "구독 정보를 불러올 수 없습니다."}</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 1, fontWeight: 800 }}>
        구독 관리
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
        현재{" "}
        <Box component="span" sx={{ color: "secondary.main", fontWeight: 700 }}>
          {subscription.currentPlan.name}
        </Box>{" "}
        플랜을 이용 중이며, {new Date(subscription.expiredAt).toLocaleDateString("ko-KR")}에 만료됩니다.
      </Typography>

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, gap: 3 }}>
        {KNOWN_PLANS.map((plan) => {
          const isCurrent = plan.name === subscription.currentPlan.name;
          return (
            <Card
              key={plan.name}
              variant="outlined"
              sx={{
                borderColor: isCurrent ? "secondary.main" : "divider",
                borderWidth: isCurrent ? 2 : 1,
                position: "relative",
              }}
            >
              {isCurrent && (
                <Chip
                  label="현재 플랜"
                  size="small"
                  color="secondary"
                  sx={{ position: "absolute", top: 16, right: 16 }}
                />
              )}
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  {plan.name}
                </Typography>
                <Typography variant="h4" sx={{ my: 1.5, fontWeight: 800 }}>
                  {plan.price === 0 ? "무료" : `₩${plan.price.toLocaleString()}`}
                  {plan.price > 0 && (
                    <Typography component="span" variant="body2" color="text.secondary">
                      {" "}
                      / 월
                    </Typography>
                  )}
                </Typography>
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1, mb: 3 }}>
                  {plan.features.map((f) => (
                    <Box key={f} sx={{ display: "flex", gap: 1, alignItems: "center" }}>
                      <CheckCircleIcon sx={{ fontSize: 18, color: "success.main" }} />
                      <Typography variant="body2">{f}</Typography>
                    </Box>
                  ))}
                </Box>
                <Button
                  fullWidth
                  variant={isCurrent ? "outlined" : "contained"}
                  color={isCurrent ? "inherit" : "secondary"}
                  disabled={isCurrent}
                  onClick={() => router.push("/mypage/payments")}
                >
                  {isCurrent ? "이용 중" : "업그레이드"}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </Box>

      {subscription.currentPlan.planTier !== "FREE" && (
        <Box sx={{ mt: 4, textAlign: "center" }}>
          <Button color="error" variant="text" onClick={() => setCancelDialogOpen(true)}>
            구독 해지 (환불 요청)
          </Button>
        </Box>
      )}

      <Dialog open={cancelDialogOpen} onClose={() => !cancelling && setCancelDialogOpen(false)}>
        <DialogTitle sx={{ fontWeight: 700 }}>구독을 해지할까요?</DialogTitle>
        <DialogContent>
          {cancelError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {cancelError}
            </Alert>
          )}
          <DialogContentText>
            사용 일수를 차감한 금액이 환불되며, 즉시 Free 플랜으로 전환됩니다. Free 플랜 범위를 초과하는
            와치리스트는 정지될 수 있습니다.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCancelDialogOpen(false)} color="inherit" disabled={cancelling}>
            취소
          </Button>
          <Button onClick={handleCancel} color="error" variant="contained" disabled={cancelling}>
            {cancelling ? <CircularProgress size={20} color="inherit" /> : "해지하기"}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
