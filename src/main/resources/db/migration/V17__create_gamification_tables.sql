CREATE TABLE achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    icon_key VARCHAR(50),
    criteria_type VARCHAR(50),
    criteria_threshold INT,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE user_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievement(id)
);
