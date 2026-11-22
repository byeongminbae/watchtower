DROP TABLE tower_notification_channel;
DROP TABLE notification_channel;
DROP SEQUENCE tower_notification_channel_id_seq;
DROP SEQUENCE notification_channel_id_seq;

DROP TABLE naver_oauth;
DELETE FROM oauth; -- 네이버에서 아직 검수를 받지 않아서 prod 에 사용자가 존재할 수 없음

ALTER TABLE oauth RENAME TO social_account;
ALTER TABLE social_account RENAME CONSTRAINT oauth_pk TO social_account_pk;
ALTER TABLE social_account RENAME CONSTRAINT oauth_member_fk TO social_account_member_fk;
ALTER TABLE social_account DROP CONSTRAINT oauth_dtype_check;
ALTER TABLE social_account
    ADD CONSTRAINT social_account_dtype_check CHECK (dtype IN ('DISCORD'));
ALTER TABLE social_account DROP COLUMN access_token;
ALTER INDEX oauth_provider_unique RENAME TO social_account_provider_uk;
ALTER SEQUENCE member_oauth_id_seq RENAME TO social_account_id_seq;

CREATE UNIQUE INDEX social_account_member_uk
    ON social_account (member_id, dtype)
    WHERE deleted_at IS NULL;

CREATE TABLE discord_account (
    id bigint CONSTRAINT discord_account_pk PRIMARY KEY
        CONSTRAINT discord_account_social_account_fk REFERENCES social_account (id),
    direct_message_channel_id character varying(255) NOT NULL
);

ALTER TABLE member DROP COLUMN email;
