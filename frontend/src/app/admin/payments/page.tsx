"use client";

import React from "react";
import {
  Alert,
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
  Paper,
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
import { PaymentHistory } from "@/types/domain";

export default function AdminPaymentsPage() {
  const [search, setSearch] = React.useState("");
  const [targetPayment, setTargetPayment] = React.useState<PaymentHistory | null>(null);
  const [cancelling, setCancelling] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);

  const { data: payments, loading, error, refetch } = useApiData(
    () => adminApi.getPayments().then((r) => r.data),
    [],
  );

  const filtered = (payments ?? []).filter((p) => p.planName.toLowerCase().includes(search.toLowerCase()));

  const handleCancel = async () => {
    if (!targetPayment) return;
    setCancelling(true);
    setActionError(null);
    try {
      await adminApi.cancelPayment(targetPayment.id);
      setTargetPayment(null);
      refetch();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "결제 취소에 실패했습니다.");
    } finally {
      setCancelling(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        결제 내역 관리
      </Typography>

      {actionError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      <TextField
        size="small"
        placeholder="플랜명으로 검색"
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
                <TableCell>결제일</TableCell>
                <TableCell>플랜</TableCell>
                <TableCell align="right">금액</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((p) => (
                <TableRow key={p.id} hover>
                  <TableCell>{new Date(p.createdAt).toLocaleDateString("ko-KR")}</TableCell>
                  <TableCell>
                    <Chip size="small" label={p.planName} color="secondary" variant="outlined" />
                  </TableCell>
                  <TableCell align="right">₩{p.planPrice.toLocaleString()}</TableCell>
                  <TableCell align="right">
                    <Button size="small" color="error" onClick={() => setTargetPayment(p)}>
                      직권 취소
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 6, color: "text.secondary" }}>
                    조회된 결제 내역이 없습니다.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={targetPayment !== null} onClose={() => !cancelling && setTargetPayment(null)}>
        <DialogTitle sx={{ fontWeight: 700 }}>결제를 직권 취소할까요?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {targetPayment?.planName} 결제 건을 취소합니다. 이 작업은 사용자에게 즉시 반영됩니다.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setTargetPayment(null)} color="inherit" disabled={cancelling}>
            취소
          </Button>
          <Button onClick={handleCancel} color="error" variant="contained" disabled={cancelling}>
            {cancelling ? <CircularProgress size={20} color="inherit" /> : "직권 취소"}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
