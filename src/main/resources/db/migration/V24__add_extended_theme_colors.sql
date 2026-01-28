-- Adiciona colunas para theming extendido (Fundo e Header/Surface)
ALTER TABLE usuario ADD COLUMN background_color VARCHAR(20) DEFAULT NULL;
ALTER TABLE usuario ADD COLUMN surface_color VARCHAR(20) DEFAULT NULL;
