ALTER TABLE tower_request_history
    ALTER COLUMN http_status_code DROP NOT NULL,
    ADD COLUMN error_code VARCHAR(255);
