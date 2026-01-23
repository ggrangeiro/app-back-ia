-- Migração para adicionar campo manager_id para hierarquia de professores
-- Professores terão manager_id apontando para o Personal que os gerencia

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS manager_id BIGINT NULL;

-- Index para consultas de subordinados (professores de um personal)
CREATE INDEX IF NOT EXISTS idx_usuario_manager ON usuario(manager_id);
