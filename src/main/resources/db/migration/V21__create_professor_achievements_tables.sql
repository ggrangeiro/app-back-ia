-- =============================================================================
-- Migration: Create Professor Achievements System
-- Tabelas separadas para conquistas de Personal Trainers/Professores
-- =============================================================================

-- Tabela de conquistas disponíveis para professores
CREATE TABLE professor_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    icon_key VARCHAR(50),
    criteria_type VARCHAR(50) NOT NULL,  -- WORKOUT_CREATED, DIET_CREATED, STUDENT_REGISTERED, ANALYSIS_PERFORMED
    criteria_threshold INT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de conquistas desbloqueadas pelos professores
CREATE TABLE professor_user_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    professor_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES professor_achievement(id),
    INDEX idx_professor_user_achievement_professor (professor_id),
    INDEX idx_professor_user_achievement_achievement (achievement_id),
    UNIQUE KEY uk_professor_achievement (professor_id, achievement_id)
);

-- =============================================================================
-- Inserção das conquistas iniciais para professores
-- =============================================================================

-- *** CONQUISTAS: TREINOS CRIADOS ***
INSERT INTO professor_achievement (name, description, icon_key, criteria_type, criteria_threshold, active) VALUES
('Primeiro Treino', 'Criou seu primeiro treino! O início de uma jornada.', 'PROF_BADGE_WORKOUT_1', 'WORKOUT_CREATED', 1, true),
('Criador de Treinos', 'Criou 10 treinos! Você está no caminho certo.', 'PROF_BADGE_WORKOUT_10', 'WORKOUT_CREATED', 10, true),
('Especialista em Treinos', 'Criou 25 treinos! Seus alunos têm muitas opções.', 'PROF_BADGE_WORKOUT_25', 'WORKOUT_CREATED', 25, true),
('Mestre dos Treinos', 'Criou 50 treinos! Experiência notável.', 'PROF_BADGE_WORKOUT_50', 'WORKOUT_CREATED', 50, true),
('Guru Fitness', 'Criou 100 treinos! Você é uma referência.', 'PROF_BADGE_WORKOUT_100', 'WORKOUT_CREATED', 100, true),
('Lenda dos Treinos', 'Criou 200 treinos! Dedicação extraordinária.', 'PROF_BADGE_WORKOUT_200', 'WORKOUT_CREATED', 200, true);

-- *** CONQUISTAS: DIETAS CRIADAS ***
INSERT INTO professor_achievement (name, description, icon_key, criteria_type, criteria_threshold, active) VALUES
('Primeira Dieta', 'Criou sua primeira dieta! Nutrição em foco.', 'PROF_BADGE_DIET_1', 'DIET_CREATED', 1, true),
('Nutricionista Iniciante', 'Criou 5 dietas! Cuidando da alimentação.', 'PROF_BADGE_DIET_5', 'DIET_CREATED', 5, true),
('Consultor Nutricional', 'Criou 15 dietas! Expertise crescente.', 'PROF_BADGE_DIET_15', 'DIET_CREATED', 15, true),
('Especialista em Nutrição', 'Criou 30 dietas! Alimentação é transformação.', 'PROF_BADGE_DIET_30', 'DIET_CREATED', 30, true),
('Mestre da Nutrição', 'Criou 50 dietas! Um verdadeiro especialista.', 'PROF_BADGE_DIET_50', 'DIET_CREATED', 50, true);

-- *** CONQUISTAS: ALUNOS CADASTRADOS ***
INSERT INTO professor_achievement (name, description, icon_key, criteria_type, criteria_threshold, active) VALUES
('Primeiro Aluno', 'Cadastrou seu primeiro aluno! A base do sucesso.', 'PROF_BADGE_STUDENT_1', 'STUDENT_REGISTERED', 1, true),
('Formador de Equipe', 'Cadastrou 5 alunos! Sua equipe está crescendo.', 'PROF_BADGE_STUDENT_5', 'STUDENT_REGISTERED', 5, true),
('Personal Popular', 'Cadastrou 15 alunos! Você está fazendo a diferença.', 'PROF_BADGE_STUDENT_15', 'STUDENT_REGISTERED', 15, true),
('Treinador Requisitado', 'Cadastrou 30 alunos! Reconhecimento merecido.', 'PROF_BADGE_STUDENT_30', 'STUDENT_REGISTERED', 30, true),
('Personal de Sucesso', 'Cadastrou 50 alunos! Sucesso profissional.', 'PROF_BADGE_STUDENT_50', 'STUDENT_REGISTERED', 50, true),
('Referência na Área', 'Cadastrou 100 alunos! Uma verdadeira referência.', 'PROF_BADGE_STUDENT_100', 'STUDENT_REGISTERED', 100, true);

-- *** CONQUISTAS: ANÁLISES REALIZADAS ***
INSERT INTO professor_achievement (name, description, icon_key, criteria_type, criteria_threshold, active) VALUES
('Primeira Análise', 'Realizou sua primeira análise de exercício!', 'PROF_BADGE_ANALYSIS_1', 'ANALYSIS_PERFORMED', 1, true),
('Analista Fitness', 'Realizou 10 análises! Olho clínico afiado.', 'PROF_BADGE_ANALYSIS_10', 'ANALYSIS_PERFORMED', 10, true),
('Especialista em Movimento', 'Realizou 25 análises! Correções precisas.', 'PROF_BADGE_ANALYSIS_25', 'ANALYSIS_PERFORMED', 25, true),
('Biomecânico Expert', 'Realizou 50 análises! Expertise em biomecânica.', 'PROF_BADGE_ANALYSIS_50', 'ANALYSIS_PERFORMED', 50, true),
('Mestre Analista', 'Realizou 100 análises! Nada escapa do seu olhar.', 'PROF_BADGE_ANALYSIS_100', 'ANALYSIS_PERFORMED', 100, true);

-- *** CONQUISTAS: AVALIAÇÕES/ANAMNESES CRIADAS ***
INSERT INTO professor_achievement (name, description, icon_key, criteria_type, criteria_threshold, active) VALUES
('Primeira Avaliação', 'Criou sua primeira avaliação física!', 'PROF_BADGE_ASSESSMENT_1', 'ASSESSMENT_CREATED', 1, true),
('Avaliador Dedicado', 'Criou 10 avaliações! Conhecendo seus alunos.', 'PROF_BADGE_ASSESSMENT_10', 'ASSESSMENT_CREATED', 10, true),
('Especialista em Avaliação', 'Criou 25 avaliações! Dados são poder.', 'PROF_BADGE_ASSESSMENT_25', 'ASSESSMENT_CREATED', 25, true),
('Mestre Avaliador', 'Criou 50 avaliações! Acompanhamento exemplar.', 'PROF_BADGE_ASSESSMENT_50', 'ASSESSMENT_CREATED', 50, true);
