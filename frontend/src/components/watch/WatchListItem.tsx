"use client";

import { Avatar, Box, ListItemButton, Stack, Typography } from "@mui/material";
import LanguageIcon from "@mui/icons-material/Language";
import { Watch } from "@/types/domain";
import WatchStatusDot from "./WatchStatusDot";

export default function WatchListItem({
  watch,
  selected,
  onClick,
}: {
  watch: Watch;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <ListItemButton
      selected={selected}
      onClick={onClick}
      sx={{
        borderRadius: 2,
        mb: 0.5,
        alignItems: "flex-start",
        py: 1.2,
        "&.Mui-selected": {
          backgroundColor: "rgba(59,130,196,0.14)",
          borderLeft: "3px solid",
          borderColor: "primary.main",
        },
      }}
    >
      <Avatar
        variant="rounded"
        src={watch.faviconUrl || undefined}
        sx={{ width: 32, height: 32, mr: 1.5, bgcolor: "background.default" }}
      >
        <LanguageIcon fontSize="small" />
      </Avatar>
      <Box sx={{ minWidth: 0, flexGrow: 1 }}>
        <Stack direction="row" spacing={1} alignItems="center">
          <WatchStatusDot status={watch.status} showLabel={false} />
          <Typography variant="body2" sx={{fontWeight: 600}}>
            {watch.name}
          </Typography>
        </Stack>
        <Typography variant="caption" color="text.secondary" noWrap sx={{ display: "block" }}>
          {watch.url}
        </Typography>
        {watch.latestAiSummary && (
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{
              display: "-webkit-box",
              WebkitLineClamp: 1,
              WebkitBoxOrient: "vertical",
              overflow: "hidden",
              mt: 0.3,
            }}
          >
            {watch.latestAiSummary}
          </Typography>
        )}
      </Box>
    </ListItemButton>
  );
}
