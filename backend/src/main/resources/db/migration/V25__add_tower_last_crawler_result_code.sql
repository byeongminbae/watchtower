ALTER TABLE tower
    ADD COLUMN last_crawler_result_code character varying(255),
    ADD CONSTRAINT tower_last_crawler_result_code_check
        CHECK (last_crawler_result_code IN ('FETCHED', 'URL_NOT_ALLOWED', 'NAVIGATION_TIMEOUT', 'TEXT_TOO_LARGE', 'NAVIGATION_FAILED', 'INVALID_SELECTOR', 'UNKNOWN'));
