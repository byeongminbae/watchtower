DELETE FROM tower_snapshot;

ALTER TABLE tower_snapshot
    ADD COLUMN html_sha256 varchar(64) NOT NULL;
