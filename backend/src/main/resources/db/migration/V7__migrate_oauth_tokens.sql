ALTER TABLE oauth RENAME CONSTRAINT oauth_pkey TO oauth_pk;
ALTER TABLE naver_oauth RENAME CONSTRAINT naver_oauth_pkey TO naver_oauth_pk;

ALTER TABLE oauth
    ADD COLUMN access_token character varying(255);

UPDATE oauth
SET access_token = naver_oauth.access_token
FROM naver_oauth
WHERE oauth.id = naver_oauth.id;

ALTER TABLE naver_oauth
    DROP COLUMN access_token;
ALTER TABLE naver_oauth RENAME COLUMN expired_at TO expires_in;

UPDATE oauth
SET deleted_at = member.deleted_at
FROM member
WHERE oauth.member_id = member.id
  AND member.deleted_at IS NOT NULL
  AND oauth.deleted_at IS NULL;
