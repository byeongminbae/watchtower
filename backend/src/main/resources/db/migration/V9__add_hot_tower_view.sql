CREATE MATERIALIZED VIEW hot_tower AS
SELECT domain, max(origin) AS url, count(*) AS change_count
FROM (
    SELECT
        lower(substring(t.url FROM '^https?://(?:[^/?#]*@)?([^/:?#]+)')) AS domain,
        lower(substring(t.url FROM '^(https?)://')) || '://'
            || lower(substring(t.url FROM '^https?://(?:[^/?#]*@)?([^/?#]+)')) AS origin
    FROM tower_request_history h
    JOIN tower t ON t.id = h.tower_id
    WHERE h.changed
      AND h.started_at >= (now() AT TIME ZONE 'UTC') - interval '1 day'
      AND t.include_in_stat
      AND t.deleted_at IS NULL
) hosts
WHERE domain IS NOT NULL
GROUP BY domain
ORDER BY change_count DESC, domain
LIMIT 10;

CREATE UNIQUE INDEX hot_tower_domain_idx ON hot_tower (domain);
