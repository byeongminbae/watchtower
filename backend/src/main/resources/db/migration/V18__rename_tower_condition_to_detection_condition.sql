ALTER TABLE tower_condition RENAME TO detection_condition;

ALTER TABLE detection_condition
    RENAME CONSTRAINT tower_condition_pk TO detection_condition_pk;
ALTER TABLE detection_condition
    RENAME CONSTRAINT tower_condition_tower_fk TO detection_condition_tower_fk;
ALTER TABLE keyword_condition
    RENAME CONSTRAINT keyword_condition_tower_condition_fk TO keyword_condition_detection_condition_fk;
