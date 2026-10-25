DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM plan WHERE plan_tier IN ('BASIC', 'PRO')) THEN
        RAISE EXCEPTION 'BASIC/PRO plans require an explicit conversion policy before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM watch WHERE status = 'PAYMENT_REQUIRED') THEN
        RAISE EXCEPTION 'PAYMENT_REQUIRED towers require an explicit conversion policy before migration';
    END IF;
END
$$;

DROP TABLE payment_cancel_history, payment_history, subscription, plan_benefit, benefit;
DROP SEQUENCE payment_cancel_history_id_seq, payment_history_id_seq,
    subscription_id_seq, plan_benefit_id_seq, benefit_id_seq;

ALTER TABLE watch
    DROP CONSTRAINT watch_status_check,
    ADD CONSTRAINT watch_status_check CHECK (status IN ('RUNNING', 'PAUSED', 'ILLEGAL_SUSPENDED'));

ALTER TABLE member RENAME CONSTRAINT member_pkey TO member_pk;
ALTER TABLE plan RENAME CONSTRAINT plan_pkey TO plan_pk;

ALTER TABLE plan RENAME COLUMN plan_tier TO type;
ALTER TABLE plan RENAME COLUMN available_from TO sale_started_at;
ALTER TABLE plan RENAME COLUMN available_until TO sale_ended_at;
ALTER TABLE plan
    DROP CONSTRAINT plan_plan_tier_check,
    ADD CONSTRAINT plan_type_check CHECK (type IN ('FREE', 'UNLIMITED')),
    DROP COLUMN price,
    DROP COLUMN duration_days,
    ADD COLUMN max_tower_quantity integer NOT NULL DEFAULT 20,
    ADD COLUMN max_condition_quantity integer NOT NULL DEFAULT 3;

UPDATE plan
SET sale_started_at = COALESCE(created_at, now() AT TIME ZONE 'UTC')
WHERE sale_started_at IS NULL;

ALTER TABLE plan
    ALTER COLUMN sale_started_at SET NOT NULL,
    ALTER COLUMN max_tower_quantity DROP DEFAULT,
    ALTER COLUMN max_condition_quantity DROP DEFAULT;

INSERT INTO plan (id, name, type, max_tower_quantity, max_condition_quantity,
                  sale_started_at, created_at, updated_at)
SELECT nextval('plan_id_seq'), '무료 플랜', 'FREE', 20, 3,
       now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE type = 'FREE' AND deleted_at IS NULL);

ALTER TABLE member
    ADD COLUMN banned_at timestamp(6) without time zone,
    ADD COLUMN plan_id bigint CONSTRAINT member_plan_fk REFERENCES plan (id);

UPDATE member
SET plan_id = (SELECT id FROM plan WHERE type = 'FREE' AND deleted_at IS NULL ORDER BY id LIMIT 1);

ALTER TABLE member
    ALTER COLUMN plan_id SET NOT NULL;
