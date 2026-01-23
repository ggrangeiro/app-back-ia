-- ============================================
-- V8: Create Structured Diet & Training Tables
-- These tables store JSON-structured data for V2 API
-- Existing dietas/treinos tables are NOT modified
-- ============================================

-- Structured Diets Table
CREATE TABLE IF NOT EXISTS structured_diets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    goal VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Summary fields (denormalized for quick access)
    total_calories INT,
    protein INT,
    carbohydrates INT,
    fats INT,
    fiber INT,
    water VARCHAR(50),
    
    -- Full structured data as JSON
    days_data JSON NOT NULL,
    
    -- Additional fields
    observations TEXT,
    
    -- Backward compatibility: HTML version for legacy mobile
    legacy_html LONGTEXT,
    
    INDEX idx_structured_diets_user (user_id),
    INDEX idx_structured_diets_created (created_at)
);

-- Structured Trainings Table
CREATE TABLE IF NOT EXISTS structured_trainings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    goal VARCHAR(50),
    level VARCHAR(20),
    frequency INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Summary fields (denormalized for quick access)
    training_style VARCHAR(100),
    estimated_duration VARCHAR(50),
    focus VARCHAR(255),
    
    -- Full structured data as JSON
    days_data JSON NOT NULL,
    
    -- Additional fields
    observations TEXT,
    
    -- Backward compatibility: HTML version for legacy mobile
    legacy_html LONGTEXT,
    
    INDEX idx_structured_trainings_user (user_id),
    INDEX idx_structured_trainings_created (created_at)
);
