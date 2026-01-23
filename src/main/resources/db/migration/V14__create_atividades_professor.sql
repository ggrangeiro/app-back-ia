-- Migração para criar tabela de rastreamento de atividades de professores
-- Registra todas as ações realizadas por professores para auditoria e produtividade

CREATE TABLE IF NOT EXISTS atividades_professor (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    professor_id    BIGINT NOT NULL,
    manager_id      BIGINT NOT NULL,
    
    -- Tipo de ação: STUDENT_CREATED, WORKOUT_GENERATED, DIET_GENERATED, 
    -- ANALYSIS_PERFORMED, ASSESSMENT_CREATED, STUDENT_EDITED, CREDITS_ADDED, PASSWORD_RESET
    action_type     VARCHAR(50) NOT NULL,
    
    -- Contexto da ação
    target_user_id  BIGINT NULL,
    target_user_name VARCHAR(255) NULL,
    resource_type   VARCHAR(50) NULL,  -- 'TRAINING' | 'DIET' | 'ANALYSIS' | 'USER' | 'ASSESSMENT'
    resource_id     BIGINT NULL,
    
    -- Metadados adicionais (JSON para flexibilidade)
    metadata        JSON NULL,
    
    -- Timestamps
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Índices para consultas frequentes
    INDEX idx_atividades_professor_id (professor_id),
    INDEX idx_atividades_manager_id (manager_id),
    INDEX idx_atividades_created (created_at DESC),
    INDEX idx_atividades_action_type (action_type)
);
