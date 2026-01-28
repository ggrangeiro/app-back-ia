CREATE TABLE group_classes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    professor_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL,
    capacity INT NOT NULL,
    location VARCHAR(255),
    photo_url VARCHAR(512),
    is_recurrent BOOLEAN DEFAULT FALSE,
    recurrence_days VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE class_bookings (
    id SERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES group_classes(id),
    student_id BIGINT NOT NULL, -- Assuming 'usuario' or 'users' is handled by ID validation in code or we add FK later if needed
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(class_id, student_id)
);
