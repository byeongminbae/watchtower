ALTER TABLE watch RENAME TO tower;
ALTER TABLE watch_condition RENAME TO tower_condition;
ALTER TABLE watch_request_history RENAME TO tower_request_history;
ALTER TABLE watch_snapshot RENAME TO tower_snapshot;

ALTER TABLE tower RENAME COLUMN interval_seconds TO fetch_interval;
ALTER TABLE tower
    ALTER COLUMN fetch_interval TYPE interval second(6)
    USING fetch_interval * interval '1 second';

ALTER TABLE tower_condition RENAME COLUMN watch_id TO tower_id;
ALTER TABLE tower_request_history RENAME COLUMN watch_id TO tower_id;
ALTER TABLE tower_snapshot RENAME COLUMN watch_id TO tower_id;

ALTER SEQUENCE watch_id_seq RENAME TO tower_id_seq;
ALTER SEQUENCE watch_condition_id_seq RENAME TO tower_condition_id_seq;
ALTER SEQUENCE watch_request_history_id_seq RENAME TO tower_request_history_id_seq;
ALTER SEQUENCE watch_snapshot_id_seq RENAME TO tower_snapshot_id_seq;

ALTER TABLE tower RENAME CONSTRAINT watch_pkey TO tower_pk;
ALTER TABLE tower RENAME CONSTRAINT watch_created_by_fk TO tower_created_by_fk;
ALTER TABLE tower RENAME CONSTRAINT watch_status_check TO tower_status_check;

ALTER TABLE tower_condition RENAME CONSTRAINT watch_condition_pkey TO tower_condition_pk;
ALTER TABLE tower_condition RENAME CONSTRAINT watch_condition_watch_fk TO tower_condition_tower_fk;
ALTER TABLE tower_condition RENAME CONSTRAINT watch_condition_dtype_check TO tower_condition_dtype_check;

ALTER TABLE tower_request_history RENAME CONSTRAINT watch_request_history_pkey TO tower_request_history_pk;
ALTER TABLE tower_request_history RENAME CONSTRAINT watch_request_history_watch_fk TO tower_request_history_tower_fk;
ALTER TABLE tower_request_history RENAME CONSTRAINT watch_request_history_triggered_by_fk TO tower_request_history_triggered_by_fk;

ALTER TABLE tower_snapshot RENAME CONSTRAINT watch_snapshot_pkey TO tower_snapshot_pk;
ALTER TABLE tower_snapshot RENAME CONSTRAINT watch_snapshot_watch_fk TO tower_snapshot_tower_fk;

ALTER TABLE html_condition RENAME CONSTRAINT html_condition_pkey TO html_condition_pk;
ALTER TABLE keyword_condition RENAME CONSTRAINT keyword_condition_pkey TO keyword_condition_pk;
ALTER TABLE regex_condition RENAME CONSTRAINT regex_condition_pkey TO regex_condition_pk;

ALTER TABLE html_condition RENAME CONSTRAINT html_condition_watch_condition_fk TO html_condition_tower_condition_fk;
ALTER TABLE keyword_condition RENAME CONSTRAINT keyword_condition_watch_condition_fk TO keyword_condition_tower_condition_fk;
ALTER TABLE regex_condition RENAME CONSTRAINT regex_condition_watch_condition_fk TO regex_condition_tower_condition_fk;

ALTER TABLE keyword_condition
    ADD COLUMN is_satisfied_at_appearance boolean NOT NULL DEFAULT true;
ALTER TABLE regex_condition
    ADD COLUMN is_satisfied_at_appearance boolean NOT NULL DEFAULT true;

ALTER TABLE tower_request_history
    ADD COLUMN changed boolean NOT NULL DEFAULT false;

CREATE INDEX tower_name_idx ON tower (name varchar_pattern_ops);
CREATE INDEX tower_request_history_started_at_idx ON tower_request_history (started_at);
