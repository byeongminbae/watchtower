ALTER TABLE keyword_condition
    ADD COLUMN satisfied_when varchar(255);

UPDATE keyword_condition
SET satisfied_when = CASE is_satisfied_at_appearance
                         WHEN true THEN 'APPEAR'
                         ELSE 'DISAPPEAR'
                     END;

ALTER TABLE keyword_condition
    ALTER COLUMN satisfied_when SET NOT NULL,
    DROP COLUMN is_satisfied_at_appearance,
    ADD CONSTRAINT keyword_condition_satisfied_when_check
        CHECK (satisfied_when IN ('APPEAR', 'DISAPPEAR'));
