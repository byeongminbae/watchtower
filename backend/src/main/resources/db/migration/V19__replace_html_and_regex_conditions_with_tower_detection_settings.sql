ALTER TABLE tower
    ADD COLUMN detect_text_change boolean NOT NULL DEFAULT false,
    ADD COLUMN detect_image_change boolean NOT NULL DEFAULT false,
    ADD COLUMN ignore_date_time boolean NOT NULL DEFAULT false,
    ADD COLUMN detection_area_selector varchar(500);

UPDATE tower
SET detect_text_change = true
WHERE EXISTS (SELECT 1
              FROM detection_condition c
                       JOIN html_condition h ON h.id = c.id
              WHERE c.tower_id = tower.id
                AND c.deleted_at IS NULL);

ALTER TABLE tower
    ALTER COLUMN detect_text_change DROP DEFAULT,
    ALTER COLUMN detect_image_change DROP DEFAULT,
    ALTER COLUMN ignore_date_time DROP DEFAULT;

DROP TABLE html_condition;
DROP TABLE regex_condition;
DELETE FROM detection_condition
WHERE dtype IN ('HTML', 'REGEX');

ALTER TABLE detection_condition
    DROP CONSTRAINT tower_condition_dtype_check,
    ADD CONSTRAINT detection_condition_dtype_check CHECK (dtype IN ('KEYWORD'));
