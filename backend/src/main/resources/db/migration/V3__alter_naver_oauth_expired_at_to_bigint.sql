ALTER TABLE naver_oauth
    ALTER COLUMN expired_at TYPE bigint
    USING (
        CASE
            WHEN expired_at IS NULL THEN NULL
            ELSE EXTRACT(EPOCH FROM expired_at)::bigint
        END
    );
