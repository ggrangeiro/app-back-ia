-- ========================================
-- MIGRATION SCRIPT: Sistema de Execução de Treinos com Cargas
-- Data: 2026-01-22
-- Autor: Backend Team
-- ========================================

-- ========================================
-- 1. TABELA: structured_workout_plans
-- Armazena treinos estruturados (V2) com JSON de exercícios
-- ========================================
CREATE TABLE IF NOT EXISTS structured_workout_plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  days_data JSON NOT NULL COMMENT 'JSON com estrutura de dias e exercícios',
  legacy_html TEXT COMMENT 'Cópia do HTML V1 para referência',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL COMMENT 'Soft delete - quando o treino foi deletado',

  FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE,

  INDEX idx_user_id (user_id),
  INDEX idx_created_at (created_at),
  INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 2. TABELA: workout_executions
-- Armazena cada execução de treino realizada pelo aluno
-- ========================================
CREATE TABLE IF NOT EXISTS workout_executions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  workout_id BIGINT NOT NULL,
  day_of_week VARCHAR(20) NOT NULL COMMENT 'monday, tuesday, wednesday, etc.',
  executed_at BIGINT NOT NULL COMMENT 'Unix timestamp em milissegundos',
  comment TEXT COMMENT 'Comentário opcional do aluno sobre o treino',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE,
  FOREIGN KEY (workout_id) REFERENCES structured_workout_plans(id) ON DELETE CASCADE,

  INDEX idx_user_workout (user_id, workout_id),
  INDEX idx_executed_at (executed_at),
  INDEX idx_user_executed_at (user_id, executed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 3. TABELA: exercise_executions
-- Armazena a execução de cada exercício individual com carga utilizada
-- ========================================
CREATE TABLE IF NOT EXISTS exercise_executions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workout_execution_id BIGINT NOT NULL,
  exercise_name VARCHAR(255) NOT NULL,
  exercise_order INT NOT NULL,
  sets_completed INT NOT NULL,
  actual_load VARCHAR(100) COMMENT 'Carga real utilizada (ex: "22kg", "Peso corporal")',
  notes TEXT COMMENT 'Observações sobre a execução do exercício',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (workout_execution_id) REFERENCES workout_executions(id) ON DELETE CASCADE,

  INDEX idx_workout_execution (workout_execution_id),
  INDEX idx_exercise_name (exercise_name),
  INDEX idx_exercise_name_workout (exercise_name, workout_execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- FIM DO SCRIPT DE MIGRAÇÃO
-- ========================================

-- NOTAS:
-- 1. Execute este script manualmente no MySQL do Google Cloud SQL
-- 2. Flyway está desabilitado neste projeto (application.yml)
-- 3. Todos os timestamps de execução usam BIGINT (Unix milissegundos) para consistência
-- 4. Soft delete implementado em structured_workout_plans (deleted_at)
-- 5. Índices criados para otimizar queries de listagem e histórico
-- 6. Tabelas de relacionamento personal/aluno/professor JÁ EXISTEM no sistema - não foram criadas aqui
