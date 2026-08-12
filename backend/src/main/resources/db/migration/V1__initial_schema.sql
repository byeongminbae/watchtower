CREATE SEQUENCE benefit_id_seq INCREMENT BY 30;
CREATE SEQUENCE member_id_seq INCREMENT BY 30;
CREATE SEQUENCE member_oauth_id_seq INCREMENT BY 30;
CREATE SEQUENCE payment_cancel_history_id_seq INCREMENT BY 30;
CREATE SEQUENCE payment_history_id_seq INCREMENT BY 30;
CREATE SEQUENCE plan_benefit_id_seq INCREMENT BY 30;
CREATE SEQUENCE plan_id_seq INCREMENT BY 30;
CREATE SEQUENCE subscription_id_seq INCREMENT BY 30;
CREATE SEQUENCE watch_condition_id_seq INCREMENT BY 30;
CREATE SEQUENCE watch_id_seq INCREMENT BY 30;
CREATE SEQUENCE watch_request_history_id_seq INCREMENT BY 30;
CREATE SEQUENCE watch_snapshot_id_seq INCREMENT BY 30;

CREATE TABLE benefit (
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    benefit_type character varying(255),
    CONSTRAINT benefit_benefit_type_check
        CHECK (benefit_type IN ('WATCH_AMOUNT'))
);

CREATE TABLE member (
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    last_login_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    email character varying(255),
    nickname character varying(255),
    profile_image_url character varying(255),
    role character varying(255),
    CONSTRAINT member_role_check
        CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE plan (
    duration_days integer NOT NULL,
    price numeric(38,2),
    available_from timestamp(6) without time zone,
    available_until timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    name character varying(255),
    plan_tier character varying(255),
    CONSTRAINT plan_plan_tier_check
        CHECK (plan_tier IN ('FREE', 'BASIC', 'PRO'))
);

CREATE TABLE oauth (
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    member_id bigint,
    updated_at timestamp(6) without time zone,
    dtype character varying(31) NOT NULL,
    provider_id character varying(255),
    CONSTRAINT oauth_dtype_check
        CHECK (dtype IN ('NAVER'))
);

CREATE TABLE payment_history (
    plan_duration_days integer NOT NULL,
    plan_price numeric(38,2),
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    member_id bigint,
    plan_name character varying(255),
    plan_tier character varying(255),
    toss_raw_response text,
    CONSTRAINT payment_history_plan_tier_check
        CHECK (plan_tier IN ('FREE', 'BASIC', 'PRO'))
);

CREATE TABLE watch (
    include_in_stat boolean NOT NULL,
    interval_seconds integer NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id bigint,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    last_fetched_at timestamp(6) without time zone,
    last_notified_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    favicon_url character varying(255),
    name character varying(255),
    status character varying(255),
    url character varying(255),
    CONSTRAINT watch_status_check
        CHECK (status IN ('RUNNING', 'PAUSED', 'PAYMENT_REQUIRED', 'ILLEGAL_SUSPENDED'))
);

CREATE TABLE watch_condition (
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    watch_id bigint,
    dtype character varying(31) NOT NULL,
    CONSTRAINT watch_condition_dtype_check
        CHECK (dtype IN ('HTML', 'KEYWORD', 'REGEX'))
);

CREATE TABLE naver_oauth (
    expired_at timestamp(6) without time zone,
    id bigint NOT NULL,
    access_token character varying(255),
    refresh_token character varying(255)
);

CREATE TABLE payment_cancel_history (
    cancel_amount integer NOT NULL,
    canceled_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    payment_history_id bigint,
    cancel_reason character varying(255),
    toss_raw_response text,
    transaction_key character varying(255)
);

CREATE TABLE plan_benefit (
    integer_value integer NOT NULL,
    benefit_id bigint,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    plan_id bigint,
    updated_at timestamp(6) without time zone
);

CREATE TABLE subscription (
    created_at timestamp(6) without time zone,
    current_plan_id bigint,
    deleted_at timestamp(6) without time zone,
    expired_at timestamp(6) without time zone,
    id bigint NOT NULL,
    member_id bigint,
    started_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone
);

CREATE TABLE watch_request_history (
    http_status_code integer NOT NULL,
    created_at timestamp(6) without time zone,
    ended_at timestamp(6) without time zone,
    id bigint NOT NULL,
    started_at timestamp(6) without time zone,
    triggered_by_id bigint,
    watch_id bigint,
    url character varying(255)
);

CREATE TABLE watch_snapshot (
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    id bigint NOT NULL,
    notified_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    watch_id bigint,
    ai_diff_summary character varying(255),
    html_content_diff_url character varying(255),
    html_content_url character varying(255),
    screenshot_diff_url character varying(255),
    screenshot_url character varying(255)
);

CREATE TABLE html_condition (
    id bigint NOT NULL
);

CREATE TABLE keyword_condition (
    id bigint NOT NULL,
    keyword character varying(255)
);

CREATE TABLE regex_condition (
    id bigint NOT NULL,
    regex character varying(255)
);

ALTER TABLE benefit
    ADD CONSTRAINT benefit_pkey PRIMARY KEY (id);
ALTER TABLE member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id);
ALTER TABLE plan
    ADD CONSTRAINT plan_pkey PRIMARY KEY (id);
ALTER TABLE oauth
    ADD CONSTRAINT oauth_pkey PRIMARY KEY (id);
ALTER TABLE payment_history
    ADD CONSTRAINT payment_history_pkey PRIMARY KEY (id);
ALTER TABLE watch
    ADD CONSTRAINT watch_pkey PRIMARY KEY (id);
ALTER TABLE watch_condition
    ADD CONSTRAINT watch_condition_pkey PRIMARY KEY (id);
ALTER TABLE naver_oauth
    ADD CONSTRAINT naver_oauth_pkey PRIMARY KEY (id);
ALTER TABLE payment_cancel_history
    ADD CONSTRAINT payment_cancel_history_pkey PRIMARY KEY (id);
ALTER TABLE plan_benefit
    ADD CONSTRAINT plan_benefit_pkey PRIMARY KEY (id);
ALTER TABLE subscription
    ADD CONSTRAINT subscription_pkey PRIMARY KEY (id);
ALTER TABLE watch_request_history
    ADD CONSTRAINT watch_request_history_pkey PRIMARY KEY (id);
ALTER TABLE watch_snapshot
    ADD CONSTRAINT watch_snapshot_pkey PRIMARY KEY (id);
ALTER TABLE html_condition
    ADD CONSTRAINT html_condition_pkey PRIMARY KEY (id);
ALTER TABLE keyword_condition
    ADD CONSTRAINT keyword_condition_pkey PRIMARY KEY (id);
ALTER TABLE regex_condition
    ADD CONSTRAINT regex_condition_pkey PRIMARY KEY (id);

ALTER TABLE subscription
    ADD CONSTRAINT subscription_member_id_key UNIQUE (member_id);

ALTER TABLE oauth
    ADD CONSTRAINT oauth_member_fk
        FOREIGN KEY (member_id) REFERENCES member (id);
ALTER TABLE payment_history
    ADD CONSTRAINT payment_history_member_fk
        FOREIGN KEY (member_id) REFERENCES member (id);
ALTER TABLE payment_cancel_history
    ADD CONSTRAINT payment_cancel_history_payment_history_fk
        FOREIGN KEY (payment_history_id) REFERENCES payment_history (id);
ALTER TABLE plan_benefit
    ADD CONSTRAINT plan_benefit_plan_fk
        FOREIGN KEY (plan_id) REFERENCES plan (id);
ALTER TABLE plan_benefit
    ADD CONSTRAINT plan_benefit_benefit_fk
        FOREIGN KEY (benefit_id) REFERENCES benefit (id);
ALTER TABLE subscription
    ADD CONSTRAINT subscription_member_fk
        FOREIGN KEY (member_id) REFERENCES member (id);
ALTER TABLE subscription
    ADD CONSTRAINT subscription_current_plan_fk
        FOREIGN KEY (current_plan_id) REFERENCES plan (id);
ALTER TABLE watch
    ADD CONSTRAINT watch_created_by_fk
        FOREIGN KEY (created_by_id) REFERENCES member (id);
ALTER TABLE watch_condition
    ADD CONSTRAINT watch_condition_watch_fk
        FOREIGN KEY (watch_id) REFERENCES watch (id);
ALTER TABLE watch_request_history
    ADD CONSTRAINT watch_request_history_watch_fk
        FOREIGN KEY (watch_id) REFERENCES watch (id);
ALTER TABLE watch_request_history
    ADD CONSTRAINT watch_request_history_triggered_by_fk
        FOREIGN KEY (triggered_by_id) REFERENCES member (id);
ALTER TABLE watch_snapshot
    ADD CONSTRAINT watch_snapshot_watch_fk
        FOREIGN KEY (watch_id) REFERENCES watch (id);
ALTER TABLE naver_oauth
    ADD CONSTRAINT naver_oauth_oauth_fk
        FOREIGN KEY (id) REFERENCES oauth (id);
ALTER TABLE html_condition
    ADD CONSTRAINT html_condition_watch_condition_fk
        FOREIGN KEY (id) REFERENCES watch_condition (id);
ALTER TABLE keyword_condition
    ADD CONSTRAINT keyword_condition_watch_condition_fk
        FOREIGN KEY (id) REFERENCES watch_condition (id);
ALTER TABLE regex_condition
    ADD CONSTRAINT regex_condition_watch_condition_fk
        FOREIGN KEY (id) REFERENCES watch_condition (id);
