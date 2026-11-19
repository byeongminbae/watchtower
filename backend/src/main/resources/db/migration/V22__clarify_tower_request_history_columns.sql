ALTER TABLE tower_request_history RENAME COLUMN url TO requested_url;
ALTER TABLE tower_request_history RENAME COLUMN started_at TO crawling_started_at;
ALTER TABLE tower_request_history RENAME COLUMN ended_at TO crawling_ended_at;
ALTER TABLE tower_request_history RENAME COLUMN http_status_code TO page_http_status_code;
ALTER TABLE tower_request_history RENAME COLUMN error_code TO crawler_result_code;

ALTER INDEX tower_request_history_started_at_idx RENAME TO tower_request_history_crawling_started_at_idx;

ALTER TABLE tower_request_history
    ALTER COLUMN crawler_result_code SET NOT NULL,
    ADD COLUMN final_url text,
    ADD COLUMN job_started_at timestamp(6) without time zone NOT NULL,
    ADD COLUMN job_ended_at timestamp(6) without time zone NOT NULL,
    ADD CONSTRAINT tower_request_history_crawler_result_code_check
        CHECK (crawler_result_code IN ('FETCHED', 'URL_NOT_ALLOWED', 'NAVIGATION_TIMEOUT', 'TEXT_TOO_LARGE', 'NAVIGATION_FAILED', 'INVALID_SELECTOR', 'UNKNOWN'));
