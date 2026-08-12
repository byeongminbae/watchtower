CREATE UNIQUE INDEX oauth_provider_unique
    ON oauth (dtype, provider_id)
    WHERE deleted_at IS NULL;
