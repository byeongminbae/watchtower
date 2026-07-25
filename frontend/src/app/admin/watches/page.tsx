"use client";

import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  Menu,
  MenuItem,
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
import React from "react";
import WatchStatusDot from "@/components/watch/WatchStatusDot";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";
import { ApiError } from "@/lib/api/errors";
import { Watch, WatchStatus } from "@/types/domain";

export default function AdminWatchesPage() {
  const [search, setSearch] = React.useState("");
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [targetWatch, setTargetWatch] = React.useState<Watch | null>(null);
  const [updating, setUpdating] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);

  const { data: watches, loading, error, refetch } = useApiData(
    () => adminApi.getWatches().then((r) => r.data),
    [],
  );

  const filtered = (watches ?? []).filter(
    (w) =>
      w.name.toLowerCase().includes(search.toLowerCase()) || w.url.toLowerCase().includes(search.toLowerCase()),
  );

  const handleOpenMenu = (e: React.MouseEvent<HTMLElement>, watch: Watch) => {
    setAnchorEl(e.currentTarget);
    setTargetWatch(watch);
  };

  const handleChangeStatus = async (status: WatchStatus) => {
    if (!targetWatch) return;
    setAnchorEl(null);
    setUpdating(true);
    setActionError(null);
    try {
      await adminApi.updateWatchStatus(targetWatch.id, status);
      refetch();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setUpdating(false);
      setTargetWatch(null);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        와치리스트 관리
      </Typography>

      {actionError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      <TextField
        size="small"
        placeholder="URL, 이름으로 검색"
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
                <TableCell>이름</TableCell>
                <TableCell>URL</TableCell>
                <TableCell>상태</TableCell>
                <TableCell>확인 주기</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((watch) => (
                <TableRow key={watch.id} hover>
                  <TableCell>{watch.name}</TableCell>
                  <TableCell sx={{ color: "text.secondary" }}>{watch.url}</TableCell>
                  <TableCell>
                    <WatchStatusDot status={watch.status} />
                  </TableCell>
                  <TableCell>{watch.intervalSeconds / 60}분</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      onClick={(e) => handleOpenMenu(e, watch)}
                      disabled={updating && targetWatch?.id === watch.id}
                    >
                      {updating && targetWatch?.id === watch.id ? (
                        <CircularProgress size={16} />
                      ) : (
                        "상태 변경"
                      )}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6, color: "text.secondary" }}>
                    조회된 와치리스트가 없습니다.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <MenuItem onClick={() => handleChangeStatus("RUNNING")}>재개</MenuItem>
        <MenuItem onClick={() => handleChangeStatus("PAUSED")}>일시정지</MenuItem>
        <MenuItem onClick={() => handleChangeStatus("ILLEGAL_SUSPENDED")} sx={{ color: "error.main" }}>
          정책 위반으로 정지
        </MenuItem>
      </Menu>
    </Container>
  );
}
