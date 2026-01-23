-- Tabela de histórico de assinaturas para auditoria
CREATE TABLE subscription_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    old_plan VARCHAR(20),
    new_plan VARCHAR(20),
    change_reason VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE
);
