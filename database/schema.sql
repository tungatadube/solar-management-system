-- Database Schema for Solar Management System
-- This file is for reference - JPA will auto-generate tables

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'TECHNICIAN', 'ASSISTANT')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Locations table
CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('WAREHOUSE', 'VEHICLE', 'JOB_SITE', 'OFFICE')),
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    contact_person VARCHAR(255),
    contact_phone VARCHAR(50),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Stock Items table
CREATE TABLE IF NOT EXISTS stock_items (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    minimum_quantity INTEGER NOT NULL DEFAULT 10,
    reorder_level INTEGER NOT NULL DEFAULT 20,
    barcode VARCHAR(100),
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Stock Locations table (junction table for stock at different locations)
CREATE TABLE IF NOT EXISTS stock_locations (
    id BIGSERIAL PRIMARY KEY,
    stock_item_id BIGINT NOT NULL REFERENCES stock_items(id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0,
    shelf VARCHAR(50),
    bin VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(stock_item_id, location_id)
);

-- Jobs table
CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    job_number VARCHAR(100) NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    client_phone VARCHAR(50),
    client_email VARCHAR(255),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    assigned_to BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED')),
    type VARCHAR(50) NOT NULL CHECK (type IN ('NEW_INSTALLATION', 'MAINTENANCE', 'REPAIR', 'INSPECTION', 'UPGRADE')),
    description TEXT,
    scheduled_start_time TIMESTAMP,
    scheduled_end_time TIMESTAMP,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    estimated_cost DECIMAL(10,2),
    actual_cost DECIMAL(10,2),
    system_size INTEGER,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Job Stock (stock used in jobs)
CREATE TABLE IF NOT EXISTS job_stock (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    stock_item_id BIGINT NOT NULL REFERENCES stock_items(id),
    quantity_used INTEGER NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    notes TEXT,
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Job Images
CREATE TABLE IF NOT EXISTS job_images (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    image_type VARCHAR(50) NOT NULL,
    caption TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Travel Logs
CREATE TABLE IF NOT EXISTS travel_logs (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    start_latitude DOUBLE PRECISION NOT NULL,
    start_longitude DOUBLE PRECISION NOT NULL,
    start_address VARCHAR(500),
    end_latitude DOUBLE PRECISION NOT NULL,
    end_longitude DOUBLE PRECISION NOT NULL,
    end_address VARCHAR(500),
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP,
    distance DECIMAL(10,2),
    duration INTEGER,
    fuel_cost DECIMAL(10,2),
    vehicle_registration VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Location Tracking (GPS tracking)
CREATE TABLE IF NOT EXISTS location_tracking (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    address VARCHAR(500),
    timestamp TIMESTAMP NOT NULL,
    device_id VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_tracking_user_timestamp ON location_tracking(user_id, timestamp);

-- Stock Movements
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGSERIAL PRIMARY KEY,
    stock_item_id BIGINT NOT NULL REFERENCES stock_items(id),
    from_location_id BIGINT REFERENCES locations(id),
    to_location_id BIGINT REFERENCES locations(id),
    movement_type VARCHAR(50) NOT NULL CHECK (movement_type IN ('STOCK_IN', 'STOCK_OUT', 'TRANSFER', 'ADJUSTMENT', 'RETURN', 'DAMAGE', 'THEFT')),
    quantity INTEGER NOT NULL,
    performed_by BIGINT NOT NULL REFERENCES users(id),
    reference VARCHAR(100),
    notes TEXT,
    movement_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_assigned_to ON jobs(assigned_to);
CREATE INDEX idx_job_stock_job_id ON job_stock(job_id);
CREATE INDEX idx_job_images_job_id ON job_images(job_id);
CREATE INDEX idx_travel_logs_job_id ON travel_logs(job_id);
CREATE INDEX idx_travel_logs_user_id ON travel_logs(user_id);

-- Sample data for testing (optional)
INSERT INTO users (username, email, password, first_name, last_name, role) VALUES
('admin', 'admin@solar.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'User', 'ADMIN'),
('john.tech', 'john@solar.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John', 'Technician', 'TECHNICIAN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO locations (name, type, address, latitude, longitude) VALUES
('Main Warehouse', 'WAREHOUSE', '123 Storage St, Adelaide SA 5000', -34.9285, 138.6007),
('Van #1', 'VEHICLE', 'Mobile', -34.9285, 138.6007)
ON CONFLICT (name) DO NOTHING;
