CREATE SEQUENCE notification_channel_id_seq INCREMENT BY 30;
CREATE SEQUENCE tower_notification_channel_id_seq INCREMENT BY 30;

CREATE TABLE notification_channel (
    id bigint CONSTRAINT notification_channel_pk PRIMARY KEY,
    member_id bigint CONSTRAINT notification_channel_member_fk REFERENCES member (id),
    name character varying(255) NOT NULL,
    type character varying(255),
    webhook_url character varying(255),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    CONSTRAINT notification_channel_type_check CHECK (type IN ('DISCORD', 'TELEGRAM'))
);

CREATE TABLE tower_notification_channel (
    id bigint CONSTRAINT tower_notification_channel_pk PRIMARY KEY,
    tower_id bigint NOT NULL CONSTRAINT tower_notification_channel_tower_fk REFERENCES tower (id),
    notification_channel_id bigint NOT NULL
        CONSTRAINT tower_notification_channel_channel_fk REFERENCES notification_channel (id),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone
);

CREATE UNIQUE INDEX tower_notification_channel_active_tower_channel_uk
    ON tower_notification_channel (tower_id, notification_channel_id)
    WHERE deleted_at IS NULL;
