-- Migration: Create geocoding_cache table for reverse geocoding API result caching
-- Date: 2025-12-26
-- Description: Adds geocoding_cache table to cache Google Maps reverse geocoding results
--              to reduce API costs and improve performance

CREATE TABLE IF NOT EXISTS geocoding_cache (
    id BIGSERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    formatted_address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    cached_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT unique_coords UNIQUE (latitude, longitude)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_geocoding_coords ON geocoding_cache(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_geocoding_expires ON geocoding_cache(expires_at);

-- Add comment to table
COMMENT ON TABLE geocoding_cache IS 'Cache for Google Maps reverse geocoding API results';
COMMENT ON COLUMN geocoding_cache.latitude IS 'Latitude rounded to 5 decimal places (~1m precision)';
COMMENT ON COLUMN geocoding_cache.longitude IS 'Longitude rounded to 5 decimal places (~1m precision)';
COMMENT ON COLUMN geocoding_cache.expires_at IS 'Cache expiration timestamp (default: 30 days from cached_at)';
