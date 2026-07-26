"use client";

import React from "react";
import Link from "next/link";
import { Alert, Box, Button, CircularProgress, Container, Paper, Stack } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { notFound, useParams } from "next/navigation";
import WatchDetailPanel from "@/components/watch/WatchDetailPanel";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

export default function WatchDetailPage() {
  const params = useParams<{ watchId: string }>();
  const watchId = Number(params.watchId);

  const {
    data: watch,
    loading,
    error,
  } = useApiData(() => watchApi.getWatch(watchId).then((r) => r.data), [watchId]);

  const { data: snapshots, loading: snapshotsLoading } = useApiData(
    () => watchApi.listWatchSnapshots(watchId).then((r) => r.data),
    [watchId],
  );

  if (Number.isNaN(watchId)) {
    notFound();
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Stack sx={{ mb: 2 }}>
        <Button
          component={Link}
          href="/watches"
          color="inherit"
          startIcon={<ArrowBackIcon />}
          sx={{ alignSelf: "flex-start", color: "text.secondary" }}
        >
          전체 와치리스트로
        </Button>
      </Stack>

      {loading && (
        <Box sx={{ display: "flex", justifyContent: "center", py: 10 }}>
          <CircularProgress color="secondary" />
        </Box>
      )}

      {!loading && error && <Alert severity="error">{error}</Alert>}

      {!loading && !error && watch && (
        <Paper variant="outlined" sx={{ p: 3, borderColor: "divider" }}>
          <WatchDetailPanel key={watch.id} watch={watch} snapshots={snapshots ?? []} snapshotsLoading={snapshotsLoading} />
        </Paper>
      )}
    </Container>
  );
}
