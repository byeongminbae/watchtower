DELETE FROM tower_notification_channel;
DELETE FROM notification_channel;

ALTER TABLE notification_channel DROP COLUMN webhook_url;
ALTER TABLE notification_channel ADD COLUMN destination_id character varying(255) NOT NULL;
