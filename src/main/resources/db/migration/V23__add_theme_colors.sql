-- Adiciona colunas para personalização de cores (Theming/Whitelabel)
ALTER TABLE usuario ADD COLUMN primary_color VARCHAR(20) DEFAULT NULL;
ALTER TABLE usuario ADD COLUMN secondary_color VARCHAR(20) DEFAULT NULL;
