-- Tabela para armazenar histórico de consumo de créditos
CREATE TABLE credit_consumption_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(20) NOT NULL,                  -- DIETA, TREINO, ANALISE
    analysis_type VARCHAR(100) NULL,             -- Tipo específico de análise (ex: BODY_COMPOSITION)
    credits_consumed INT NOT NULL DEFAULT 1,
    was_free BOOLEAN DEFAULT FALSE,              -- Indica se usou geração gratuita (Starter) ou plano PRO/STUDIO
    credit_source VARCHAR(20) NOT NULL,          -- SUBSCRIPTION, PURCHASED, FREE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_cch_user_id (user_id),
    INDEX idx_cch_created_at (created_at),
    INDEX idx_cch_reason (reason),
    FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE
);
