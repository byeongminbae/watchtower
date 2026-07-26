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
  const { member } = useAuth();
  const {
    data: payments,
    loading,
    error,
  } = useApiData(
    () => (member ? memberApi.getMemberPaymentHistories(member.id).then((r) => r.data) : Promise.resolve([])),
    [member?.id],
  );

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        결제 이력
      </Typography>

      {loading && (
        <Box sx={{ display: "flex", justifyContent: "center", py: 10 }}>
          <CircularProgress color="secondary" />
        </Box>
      )}

      {!loading && error && <Alert severity="error">{error}</Alert>}

      {!loading && !error && (
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
