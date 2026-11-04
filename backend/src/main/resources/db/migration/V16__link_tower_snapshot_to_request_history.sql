DELETE FROM tower_snapshot;

DROP MATERIALIZED VIEW hot_tower;

ALTER TABLE tower_request_history
    DROP COLUMN changed;

ALTER TABLE tower_snapshot RENAME COLUMN changed TO is_condition_satisfied;

ALTER TABLE tower_snapshot
    ADD COLUMN tower_request_history_id bigint NOT NULL,
    ADD CONSTRAINT tower_snapshot_tower_request_history_fk
        FOREIGN KEY (tower_request_history_id) REFERENCES tower_request_history (id),
    ADD CONSTRAINT tower_snapshot_tower_request_history_uk UNIQUE (tower_request_history_id);

CREATE MATERIALIZED VIEW hot_tower AS
SELECT domain, max(origin) AS url, count(*) AS change_count
FROM (
    SELECT
        lower(substring(t.url FROM '^https?://(?:[^/?#]*@)?([^/:?#]+)')) AS domain,
        lower(substring(t.url FROM '^(https?)://')) || '://'
            || lower(substring(t.url FROM '^https?://(?:[^/?#]*@)?([^/?#]+)')) AS origin
    FROM tower_request_history h
    JOIN tower_snapshot s ON s.tower_request_history_id = h.id
    JOIN tower t ON t.id = h.tower_id
    WHERE s.is_condition_satisfied
      AND h.started_at >= (now() AT TIME ZONE 'UTC') - interval '1 day'
      AND t.include_in_stat
      AND t.deleted_at IS NULL
) hosts
WHERE domain IS NOT NULL
GROUP BY domain
ORDER BY change_count DESC, domain
LIMIT 10;

CREATE UNIQUE INDEX hot_tower_domain_idx ON hot_tower (domain);
