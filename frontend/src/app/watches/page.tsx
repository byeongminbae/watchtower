"use client";

import { Alert, Box, Button, CircularProgress, Container, Grid, List, Paper, Stack, TextField, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import InboxOutlinedIcon from "@mui/icons-material/InboxOutlined";
import InputAdornment from "@mui/material/InputAdornment";
import React from "react";
import WatchListItem from "@/components/watch/WatchListItem";
import WatchDetailPanel from "@/components/watch/WatchDetailPanel";
import CreateWatchDialog from "@/components/watch/CreateWatchDialog";
import { memberApi, watchApi } from "@/lib/api";
import { CreateWatchRequest } from "@/lib/api/watch";
import { useAuth } from "@/lib/AuthContext";
import { useApiData } from "@/lib/useApiData";

export default function WatchesPage() {
  const { principal } = useAuth();
  const memberId = principal?.memberId;
  // 사용자가 명시적으로 다른 와치를 선택했을 때만 값이 채워짐. null이면 "첫 번째 와치를 기본 선택"으로 간주(렌더 시점에 파생).
  const [manualSelectedId, setManualSelectedId] = React.useState<number | null>(null);
  const [dialogOpen, setDialogOpen] = React.useState(false);
  const [search, setSearch] = React.useState("");

  const {
    data: watches,
    loading,
    error,
    refetch,
  } = useApiData(
    () =>
      memberId === undefined
        ? new Promise<never>(() => undefined)
        : memberApi.getMemberWatches(memberId).then((r) => r.data),
    [memberId],
  );

  const filteredWatches = (watches ?? []).filter(
    (w) => w.name.toLowerCase().includes(search.toLowerCase()) || w.url.toLowerCase().includes(search.toLowerCase()),
  );

  // effect 없이 렌더 시점에 파생: 수동 선택이 없거나 그 와치가 목록에서 사라졌으면 첫 번째 와치로 대체
  const selectedWatch =
    (watches ?? []).find((w) => w.id === manualSelectedId) ?? (watches && watches.length > 0 ? watches[0] : undefined);

  const {
    data: snapshots,
    loading: snapshotsLoading,
  } = useApiData(
    () => (selectedWatch ? watchApi.listWatchSnapshots(selectedWatch.id).then((r) => r.data) : Promise.resolve([])),
    [selectedWatch?.id],
  );

  const handleCreate = async (payload: CreateWatchRequest) => {
    await watchApi.createWatch(payload);
    refetch();
  };

  if (memberId === undefined || loading || (watches === null && !error)) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
        <CircularProgress color="secondary" size={28} />
      </Box>
    );
  }

  if (error) {
    return (
      <Container maxWidth="sm" sx={{ py: 10 }}>
        <Alert severity="error">{error}</Alert>
      </Container>
    );
  }

  if ((watches ?? []).length === 0) {
    return (
      <Container maxWidth="sm" sx={{ py: 16, textAlign: "center" }}>
        <InboxOutlinedIcon sx={{ fontSize: 64, color: "text.secondary", mb: 2 }} />
        <Typography variant="h6" sx={{ fontWeight: 700 }}>
          아직 등록된 와치리스트가 없어요
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          지켜보고 싶은 URL을 등록하면 변화가 생겼을 때 바로 알려드릴게요.
        </Typography>
        <Button
          variant="contained"
          color="secondary"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
        >
          첫 와치리스트 등록하기
        </Button>
        <CreateWatchDialog open={dialogOpen} onClose={() => setDialogOpen(false)} onSubmit={handleCreate} />
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Grid container spacing={3}>
        {/* 좌측: 와치리스트 목록 */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Stack spacing={2} sx={{ mb: 2 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                와치리스트
              </Typography>
              <Button
                size="small"
                variant="contained"
                color="secondary"
                startIcon={<AddIcon />}
                onClick={() => setDialogOpen(true)}
              >
                등록
              </Button>
            </Stack>
            <TextField
              size="small"
              placeholder="이름 또는 URL 검색"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" />
                  </InputAdornment>
                ),
              }}
            />
          </Stack>
          <Paper
            variant="outlined"
            sx={{ p: 1, maxHeight: "70vh", overflowY: "auto", borderColor: "divider" }}
          >
            <List disablePadding>
              {filteredWatches.map((watch) => (
                <WatchListItem
                  key={watch.id}
                  watch={watch}
                  selected={watch.id === selectedWatch?.id}
                  onClick={() => setManualSelectedId(watch.id)}
                />
              ))}
            </List>
          </Paper>
        </Grid>

        {/* 우측: 상세 패널 */}
        <Grid size={{ xs: 12, md: 8 }}>
          {selectedWatch ? (
            <Paper variant="outlined" sx={{ p: 3, borderColor: "divider" }}>
              <WatchDetailPanel
                key={selectedWatch.id}
                watch={selectedWatch}
                snapshots={snapshots ?? []}
                snapshotsLoading={snapshotsLoading}
                onChanged={refetch}
              />
            </Paper>
          ) : (
            <Box sx={{ textAlign: "center", py: 10, color: "text.secondary" }}>
              왼쪽에서 와치리스트를 선택해주세요.
            </Box>
          )}
        </Grid>
      </Grid>

      <CreateWatchDialog open={dialogOpen} onClose={() => setDialogOpen(false)} onSubmit={handleCreate} />
    </Container>
  );
}
