-- Index daily tracking tables for weekly review range queries
-- These indexes support WeeklyReviewMapper queries with (user_id, local_date BETWEEN start AND end)

CREATE INDEX idx_daily_metric_user_date
    ON daily_metric (user_id, local_date);

CREATE INDEX idx_nutrition_log_user_date
    ON nutrition_log (user_id, local_date);

-- training_log already has idx_training_log_user_date with (user_id, local_date, created_at, id) from V7
-- which covers the weekly review query efficiently
