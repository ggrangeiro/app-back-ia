-- Script para criar tabela de integração EVO no banco de dados FitAI
-- Executar este script no banco MySQL

-- Tabela de integração EVO por academia/personal
CREATE TABLE IF NOT EXISTS evo_integration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,           -- ID do Personal/Academia no FitAI
    evo_username VARCHAR(255) NOT NULL,       -- Usuário da API EVO
    evo_password VARCHAR(512) NOT NULL,       -- Senha (criptografada idealmente)
    evo_base_url VARCHAR(255),                -- URL customizada ou null para padrão
    evo_branch_id VARCHAR(50),                -- ID da filial no EVO
    status VARCHAR(50) DEFAULT 'PENDING',     -- PENDING, ACTIVE, ERROR, INACTIVE
    error_message TEXT,                       -- Última mensagem de erro
    sync_members BOOLEAN DEFAULT TRUE,        -- Sincronizar membros?
    sync_employees BOOLEAN DEFAULT TRUE,      -- Sincronizar funcionários?
    sync_workouts BOOLEAN DEFAULT TRUE,       -- Sincronizar treinos?
    auto_sync BOOLEAN DEFAULT FALSE,          -- Sincronização automática?
    last_members_sync TIMESTAMP,              -- Última sincronização de membros
    last_employees_sync TIMESTAMP,            -- Última sincronização de funcionários
    last_workouts_sync TIMESTAMP,             -- Última sincronização de treinos
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_evo_user_id (user_id),
    INDEX idx_evo_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Adicionar campos EVO na tabela usuario (se ainda não existirem)
ALTER TABLE usuario 
    ADD COLUMN IF NOT EXISTS evo_member_id VARCHAR(50) AFTER surface_color,
    ADD COLUMN IF NOT EXISTS evo_branch_id VARCHAR(50) AFTER evo_member_id,
    ADD COLUMN IF NOT EXISTS evo_last_sync TIMESTAMP AFTER evo_branch_id;

-- Índices para busca por campos EVO
CREATE INDEX IF NOT EXISTS idx_usuario_evo_member_id ON usuario(evo_member_id);
CREATE INDEX IF NOT EXISTS idx_usuario_evo_branch_id ON usuario(evo_branch_id);

-- =====================================================
-- FASE 5: Tabela para mapeamento de exercícios FitAI → EVO
-- =====================================================

CREATE TABLE IF NOT EXISTS evo_exercise_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,                          -- ID do Personal/Academia dono do mapeamento
    fitai_exercise_name VARCHAR(255) NOT NULL,        -- Nome original do exercício no FitAI
    fitai_exercise_name_normalized VARCHAR(255) NOT NULL, -- Nome normalizado para busca
    evo_exercise_id BIGINT NOT NULL,                  -- ID do exercício no EVO
    evo_exercise_name VARCHAR(255),                   -- Nome do exercício no EVO
    muscle_group VARCHAR(100),                        -- Grupo muscular
    match_score DOUBLE,                               -- Score de correspondência (para match automático)
    is_verified BOOLEAN DEFAULT FALSE,                -- Se foi verificado manualmente
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Índice único para garantir um mapeamento por exercício por usuário
    UNIQUE KEY uk_evo_mapping_user_exercise (user_id, fitai_exercise_name_normalized),
    INDEX idx_evo_mapping_user_id (user_id),
    INDEX idx_evo_mapping_evo_id (evo_exercise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- FASE 4: Adicionar campos EVO na tabela structured_treino
-- Para rastrear treinos exportados para o EVO
-- =====================================================

ALTER TABLE structured_treino 
    ADD COLUMN IF NOT EXISTS evo_workout_id VARCHAR(255) AFTER observations,
    ADD COLUMN IF NOT EXISTS evo_synced_at TIMESTAMP AFTER evo_workout_id;

-- Índice para buscar treinos exportados
CREATE INDEX IF NOT EXISTS idx_structured_treino_evo_workout_id ON structured_treino(evo_workout_id);

-- Comentário: Para produção, considerar usar AWS KMS ou HashiCorp Vault
-- para criptografar a senha da API EVO antes de armazenar

