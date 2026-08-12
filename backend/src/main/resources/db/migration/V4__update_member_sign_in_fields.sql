ALTER TABLE member
    RENAME COLUMN last_login_at TO last_sign_in_at;

ALTER TABLE member
    ADD COLUMN refresh_token character varying(255);

ALTER TABLE member
    DROP CONSTRAINT member_role_check;

UPDATE member
SET role = 'NORMAL'
WHERE role = 'USER';

ALTER TABLE member
    ADD CONSTRAINT member_role_check
        CHECK (role IN ('NORMAL', 'ADMIN'));
