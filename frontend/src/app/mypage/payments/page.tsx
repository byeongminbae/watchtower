"use client";

import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Container,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import { useAuth } from "@/lib/AuthContext";
import { memberApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

export default function PaymentsPage() {
  const { principal } = useAuth();
  const memberId = principal?.memberId;
  const {
    data: payments,
    loading,
    error,
  } = useApiData(
    () =>
      memberId === undefined
        ? new Promise<never>(() => undefined)
        : memberApi.getMemberPaymentHistories(memberId).then((r) => r.data),
    [memberId],
  );
  const pending = memberId === undefined || loading || (payments === null && !error);

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        결제 이력
      </Typography>

      {pending && (
        <Box sx={{ display: "flex", justifyContent: "center", py: 10 }}>
          <CircularProgress color="secondary" />
        </Box>
      )}

      {!pending && error && <Alert severity="error">{error}</Alert>}

      {!pending && !error && (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>결제일</TableCell>
                <TableCell>플랜</TableCell>
                <TableCell align="right">결제 금액</TableCell>
                <TableCell align="right">이용 기간</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(payments ?? []).map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{new Date(p.createdAt).toLocaleDateString("ko-KR")}</TableCell>
                  <TableCell>
                    <Chip size="small" label={p.planName} color="secondary" variant="outlined" />
                  </TableCell>
                  <TableCell align="right">₩{p.planPrice.toLocaleString()}</TableCell>
                  <TableCell align="right">{p.planDurationDays}일</TableCell>
                </TableRow>
              ))}
              {(payments ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 6, color: "text.secondary" }}>
                    결제 이력이 없습니다.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Container>
  );
}
