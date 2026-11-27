CREATE INDEX tower_snapshot_tower_id_created_at_idx
    ON tower_snapshot (tower_id, created_at)
    WHERE deleted_at IS NULL;
