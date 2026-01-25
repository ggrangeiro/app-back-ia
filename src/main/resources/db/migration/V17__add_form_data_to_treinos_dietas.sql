-- V17: Adiciona campo form_data para persistir dados do formulário de geração
-- Permite refazer treinos e dietas usando os mesmos parâmetros originais

-- Adiciona coluna form_data na tabela treinos
ALTER TABLE treinos ADD COLUMN form_data TEXT;

-- Adiciona coluna form_data na tabela dietas
ALTER TABLE dietas ADD COLUMN form_data TEXT;
