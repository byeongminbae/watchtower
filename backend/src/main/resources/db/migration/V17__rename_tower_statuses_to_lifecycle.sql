ALTER TABLE tower
    DROP CONSTRAINT tower_status_check;

UPDATE tower
SET status = CASE status
                 WHEN 'CRAWLING_READY' THEN 'READY'
                 WHEN 'CRAWLING_ERROR' THEN 'ERROR'
                 WHEN 'CRAWLING' THEN 'RUNNING'
                 WHEN 'USER_PAUSED' THEN 'PAUSED'
                 WHEN 'ADMIN_PAUSED' THEN 'SUSPENDED'
             END;

ALTER TABLE tower
    ADD CONSTRAINT tower_status_check
        CHECK (status IN ('READY', 'ERROR', 'RUNNING', 'PAUSED', 'SUSPENDED'));
