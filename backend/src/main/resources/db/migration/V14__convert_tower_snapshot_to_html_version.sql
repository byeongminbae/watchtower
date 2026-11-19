ALTER TABLE tower_snapshot
    ADD COLUMN changed boolean NOT NULL DEFAULT false;

UPDATE tower_snapshot
SET changed = true
WHERE html_content_diff_url IS NOT NULL;

ALTER TABLE tower_snapshot
    ALTER COLUMN changed DROP DEFAULT,
    DROP COLUMN html_content_diff_url;

ALTER TABLE tower_snapshot RENAME COLUMN html_content_url TO html_object_name;
ALTER TABLE tower_snapshot RENAME COLUMN screenshot_url TO screenshot_object_name;
ALTER TABLE tower_snapshot RENAME COLUMN screenshot_diff_url TO highlighted_screenshot_object_name;

ALTER TABLE tower_snapshot
    ALTER COLUMN html_object_name SET NOT NULL;
