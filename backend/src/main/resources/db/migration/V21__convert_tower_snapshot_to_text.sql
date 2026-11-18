ALTER TABLE tower_snapshot RENAME COLUMN html_object_name TO text_object_name;
ALTER TABLE tower_snapshot RENAME COLUMN html_sha256 TO text_hash;
ALTER TABLE tower_snapshot RENAME COLUMN is_condition_satisfied TO is_change_detected;

ALTER TABLE tower_snapshot
    ADD COLUMN date_time_ignored_text_hash varchar(64) NOT NULL,
    ADD COLUMN detection_area_selector varchar(500),
    ADD COLUMN image_hash varchar(64);
