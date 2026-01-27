CREATE TABLE professor_exercise_videos (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT NOT NULL,
    exercise_id VARCHAR(255) NOT NULL,
    video_url TEXT NOT NULL,
    video_type VARCHAR(50) DEFAULT 'YOUTUBE',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_professor_exercise_videos UNIQUE (professor_id, exercise_id)
);

CREATE INDEX idx_professor_exercise_videos_professor_id ON professor_exercise_videos(professor_id);
CREATE INDEX idx_professor_exercise_videos_exercise_id ON professor_exercise_videos(exercise_id);
