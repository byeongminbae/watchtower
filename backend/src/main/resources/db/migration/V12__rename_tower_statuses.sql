ALTER TABLE tower
    DROP CONSTRAINT tower_status_check;

UPDATE tower
SET status = CASE status
                 WHEN 'RUNNING' THEN 'CRAWLING_READY'
                 WHEN 'PAUSED' THEN 'USER_PAUSED'
                 WHEN 'ILLEGAL_SUSPENDED' THEN 'ADMIN_PAUSED'
             END;

ALTER TABLE tower
    ADD CONSTRAINT tower_status_check
        CHECK (status IN ('CRAWLING_READY', 'CRAWLING_ERROR', 'CRAWLING', 'USER_PAUSED', 'ADMIN_PAUSED'));
