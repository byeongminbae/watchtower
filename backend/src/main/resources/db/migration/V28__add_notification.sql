CREATE SEQUENCE notification_id_seq INCREMENT BY 30;

CREATE TABLE notification (
    id bigint CONSTRAINT notification_pk PRIMARY KEY,
    recipient_id bigint NOT NULL
        CONSTRAINT notification_recipient_fk REFERENCES member (id),
    content text NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone
);

CREATE SEQUENCE notification_delivery_id_seq INCREMENT BY 30;

CREATE TABLE notification_delivery (
    id bigint CONSTRAINT notification_delivery_pk PRIMARY KEY,
    notification_id bigint NOT NULL
        CONSTRAINT notification_delivery_notification_fk REFERENCES notification (id),
    social_account_id bigint NOT NULL
        CONSTRAINT notification_delivery_social_account_fk REFERENCES social_account (id),
    next_attempt_at timestamp(6) without time zone NOT NULL,
    failed_attempt_count integer NOT NULL,
    succeeded_at timestamp(6) without time zone,
    abandoned_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone
);

CREATE INDEX notification_delivery_sendable_idx
    ON notification_delivery (next_attempt_at)
    WHERE succeeded_at IS NULL AND abandoned_at IS NULL AND deleted_at IS NULL;

ALTER TABLE tower_snapshot
    ADD COLUMN notification_id bigint
        CONSTRAINT tower_snapshot_notification_fk REFERENCES notification (id)
        CONSTRAINT tower_snapshot_notification_uk UNIQUE;
